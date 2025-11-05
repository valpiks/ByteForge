#include "sandbox.h"
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/resource.h>
#include <sys/stat.h>
#include <sys/time.h>
#include <sys/socket.h>
#include <sys/poll.h>
#include <fcntl.h>
#include <unistd.h>
#include <signal.h>
#include <errno.h>
#include <cstring>
#include <fstream>
#include <sstream>
#include <filesystem>
#include <chrono>
#include <thread>
#include <algorithm>
#include <vector>
#include <map>
#include <iostream>
#include <atomic>

using namespace std::chrono;

std::string escape_json_str(const std::string &in)
{
	std::string out;
	out.reserve(in.size());
	for (char c : in)
	{
		switch (c)
		{
		case '"':
			out += "\\\"";
			break;
		case '\\':
			out += "\\\\";
			break;
		case '\b':
			out += "\\b";
			break;
		case '\f':
			out += "\\f";
			break;
		case '\n':
			out += "\\n";
			break;
		case '\r':
			out += "\\r";
			break;
		case '\t':
			out += "\\t";
			break;
		default:
			out += c;
			break;
		}
	}
	return out;
}

class InteractiveSandbox::InteractiveSandboxImpl
{
private:
	std::string temp_dir;
	int time_limit_sec;
	int memory_limit_mb;
	int client_fd;
	std::atomic<bool> program_finished{false};
	std::atomic<bool> waiting_for_input{false};
	std::atomic<bool> input_sent{false};
	time_point<steady_clock> start_time;

	void send_raw(const std::string &s)
	{
		size_t off = 0;
		while (off < s.size())
		{
			ssize_t n = ::send(client_fd, s.data() + off, s.size() - off, 0);
			if (n <= 0)
				break;
			off += (size_t)n;
		}
	}

	void send_json_result(int exit_code, const std::string &output, const std::string &error,
												bool timed_out = false, bool memory_exceeded = false)
	{
		auto end_time = steady_clock::now();
		auto ms = duration_cast<milliseconds>(end_time - start_time).count();

		std::string final_error = error;
		std::string final_output = output;

		if (timed_out)
		{
			final_error = "Time limit exceeded (" + std::to_string(time_limit_sec) + "s)";
			exit_code = -4;
		}
		else if (memory_exceeded)
		{
			final_error = "Memory limit exceeded (" + std::to_string(memory_limit_mb) + "MB)";
			exit_code = -5;
		}

		std::string status = "SUCCESS";
		if (exit_code != 0)
			status = "RUNTIME_ERROR";
		if (timed_out)
			status = "TIME_LIMIT_EXCEEDED";
		if (memory_exceeded)
			status = "MEMORY_LIMIT_EXCEEDED";
		if (!final_error.empty() && exit_code == -2)
			status = "COMPILATION_ERROR";
		if (exit_code == -3)
			status = "SECURITY_ERROR";

		std::string j = "{\"type\":\"EXECUTION_RESULT\",\"output\":\"" + escape_json_str(final_output) +
										"\",\"error\":\"" + escape_json_str(final_error) + "\",\"status\":\"" + status +
										"\",\"exit_code\":" + std::to_string(exit_code) + ",\"execution_time_ms\":" +
										std::to_string(ms) + ",\"memory_used_kb\":0,\"timed_out\":" +
										(timed_out ? "true" : "false") + ",\"memory_exceeded\":" +
										(memory_exceeded ? "true" : "false") + "}";
		send_raw(j);
	}

	void send_compile_success()
	{
		std::string j = "{\"type\":\"COMPILE_SUCCESS\",\"message\":\"Code compiled successfully\"}";
		send_raw(j);
	}

	void send_program_output(const std::string &out)
	{
		std::string j = "{\"type\":\"OUTPUT\",\"message\":\"" + escape_json_str(out) + "\"}";
		send_raw(j);
	}

	void send_input_request(const std::string &prompt)
	{
		std::string j = "{\"type\":\"INPUT_REQUIRED\",\"message\":\"" + escape_json_str(prompt) + "\"}";
		send_raw(j);
		waiting_for_input = true;
		input_sent = false;
	}

	void send_error_message(const std::string &msg, int code)
	{
		std::string j = "{\"type\":\"ERROR\",\"message\":\"" + escape_json_str(msg) +
										"\",\"exit_code\":" + std::to_string(code) + "}";
		send_raw(j);
	}

	bool is_dangerous_code(const std::string &code)
	{
		std::vector<std::string> patterns = {
				"system(", "exec(", "popen(", "CreateProcess", "ShellExecute",
				"WinExec", "fork(", "chmod", "rm -rf", "format", "shutdown"};
		std::string low = code;
		std::transform(low.begin(), low.end(), low.begin(), ::tolower);
		for (auto &p : patterns)
			if (low.find(p) != std::string::npos)
				return true;
		return false;
	}

	bool is_dangerous_map(const std::map<std::string, std::string> &m)
	{
		for (auto &kv : m)
			if (is_dangerous_code(kv.second))
				return true;
		return false;
	}

	void log(const std::string &message)
	{
		auto now = std::chrono::system_clock::now();
		auto time_t = std::chrono::system_clock::to_time_t(now);
		char time_str[9];
		std::strftime(time_str, sizeof(time_str), "%H:%M:%S", std::localtime(&time_t));
		std::cout << "[SANDBOX][" << time_str << "] " << message << std::endl;
	}

	void log_error(const std::string &message)
	{
		auto now = std::chrono::system_clock::now();
		auto time_t = std::chrono::system_clock::to_time_t(now);
		char time_str[9];
		std::strftime(time_str, sizeof(time_str), "%H:%M:%S", std::localtime(&time_t));
		std::cerr << "[SANDBOX][" << time_str << "] ❌ " << message << std::endl;
	}

public:
	InteractiveSandboxImpl(const std::string &dir, int tlim = 5, int mlim = 256)
			: temp_dir(dir), time_limit_sec(tlim), memory_limit_mb(mlim), client_fd(-1)
	{
		std::filesystem::create_directories(temp_dir);
		log("Created sandbox: " + temp_dir + " (limits: " + std::to_string(time_limit_sec) +
				"s, " + std::to_string(memory_limit_mb) + "MB)");
	}

	~InteractiveSandboxImpl() { cleanup(); }

	void cleanup()
	{
		try
		{
			if (std::filesystem::exists(temp_dir))
			{
				std::filesystem::remove_all(temp_dir);
				log("Cleaned up: " + temp_dir);
			}
		}
		catch (...)
		{
			log_error("Failed to clean up: " + temp_dir);
		}
	}

	bool compile_code_utf8(const std::string &code, std::string &error)
	{
		log("Compiling single file...");
		std::ofstream src(temp_dir + "/program.cpp", std::ios::binary);
		if (!src)
		{
			error = "Cannot create source file";
			return false;
		}
		unsigned char bom[] = {0xEF, 0xBB, 0xBF};
		src.write(reinterpret_cast<char *>(bom), sizeof(bom));
		src << code;
		src.close();

		std::string cmd = "cd \"" + temp_dir + "\" && g++ -std=c++17 -finput-charset=UTF-8 -fexec-charset=UTF-8 program.cpp -o program 2> compile_errors.txt";
		log("Compile command: " + cmd);

		int rc = system(cmd.c_str());
		std::ifstream ef(temp_dir + "/compile_errors.txt", std::ios::binary);
		if (ef)
		{
			std::string line;
			while (std::getline(ef, line))
			{
				error += line + "\n";
			}
			ef.close();
		}

		std::string program_path = temp_dir + "/program";
		bool ok = (rc == 0) && std::filesystem::exists(program_path);

		if (ok)
		{
			chmod(program_path.c_str(), 0755);
			log("Compilation successful");
		}
		else
		{
			log_error("Compilation failed: " + error);
		}
		return ok;
	}

	bool compile_multiple_files(const std::map<std::string, std::string> &files, std::string &error)
	{
		log("Compiling " + std::to_string(files.size()) + " files...");
		std::vector<std::string> cpp_list;
		bool has_main = false;
		int idx = 0;

		for (auto &kv : files)
		{
			std::string name = kv.first;
			std::string safe = name;
			std::replace(safe.begin(), safe.end(), '\\', '/');

			bool nonascii = false;
			for (char c : safe)
				if ((unsigned char)c > 127)
				{
					nonascii = true;
					break;
				}

			std::string actual = safe;
			if (nonascii)
			{
				size_t p = safe.find_last_of('.');
				std::string ext = (p == std::string::npos) ? ".cpp" : safe.substr(p);
				actual = "file_" + std::to_string(idx++) + ext;
			}

			std::filesystem::path fp = std::filesystem::path(temp_dir) / actual;
			if (fp.has_parent_path())
				std::filesystem::create_directories(fp.parent_path());

			std::ofstream ofs(fp, std::ios::binary);
			if (!ofs)
			{
				error = "Cannot create file: " + actual;
				return false;
			}

			if (actual.find(".cpp") != std::string::npos ||
					actual.find(".cxx") != std::string::npos ||
					actual.find(".cc") != std::string::npos)
			{

				unsigned char bom[] = {0xEF, 0xBB, 0xBF};
				ofs.write(reinterpret_cast<char *>(bom), sizeof(bom));

				std::string content = kv.second;
				bool file_has_main = content.find("int main(") != std::string::npos ||
														 content.find("void main(") != std::string::npos ||
														 content.find("main()") != std::string::npos;

				if (file_has_main && !has_main)
				{
					cpp_list.insert(cpp_list.begin(), actual);
					has_main = true;
				}
				else
				{
					cpp_list.push_back(actual);
				}
			}

			ofs << kv.second;
			ofs.close();
			log("Created file: " + actual);
		}

		if (cpp_list.empty())
		{
			error = "No C++ source files found";
			return false;
		}

		std::string cmd = "cd \"" + temp_dir + "\" && g++ -std=c++17 -finput-charset=UTF-8 -fexec-charset=UTF-8 ";
		for (auto &p : cpp_list)
			cmd += "\"" + p + "\" ";
		cmd += "-o program 2> compile_errors.txt";

		log("Multi-file compile command: " + cmd);
		int rc = system(cmd.c_str());
		std::ifstream ef(temp_dir + "/compile_errors.txt", std::ios::binary);
		if (ef)
		{
			std::string line;
			while (std::getline(ef, line))
			{
				error += line + "\n";
			}
			ef.close();
		}

		std::string program_path = temp_dir + "/program";
		bool ok = (rc == 0) && std::filesystem::exists(program_path);

		if (ok)
		{
			chmod(program_path.c_str(), 0755);
			log("Multi-file compilation successful");
		}
		else
		{
			log_error("Multi-file compilation failed: " + error);
		}
		return ok;
	}

	void execute_with_pipes(const std::string &code, int sockfd)
	{
		log("Starting single file execution...");
		client_fd = sockfd;
		program_finished = false;
		waiting_for_input = false;
		input_sent = false;
		start_time = steady_clock::now();

		if (is_dangerous_code(code))
		{
			log_error("Dangerous code detected - execution blocked");
			send_error_message("Dangerous code detected: execution blocked", -3);
			return;
		}

		std::string compile_error;
		if (!compile_code_utf8(code, compile_error))
		{
			log_error("Compilation failed");
			send_error_message("Compilation failed: " + compile_error, -2);
			return;
		}

		log("Sending compile success");
		send_compile_success();

		log("Running program...");
		run_program_with_pipes();
		log("Program execution completed");
	}

	void execute_multiple_files_with_pipes(const std::map<std::string, std::string> &files, int sockfd)
	{
		log("Starting multi-file execution...");
		client_fd = sockfd;
		program_finished = false;
		waiting_for_input = false;
		input_sent = false;
		start_time = steady_clock::now();

		if (is_dangerous_map(files))
		{
			log_error("Dangerous code detected - execution blocked");
			send_error_message("Dangerous code detected: execution blocked", -3);
			return;
		}

		std::string compile_error;
		if (!compile_multiple_files(files, compile_error))
		{
			log_error("Multi-file compilation failed");
			send_error_message("Compilation failed: " + compile_error, -2);
			return;
		}

		log("Sending compile success");
		send_compile_success();

		log("Running program...");
		run_program_with_pipes();
		log("Program execution completed");
	}

private:
	void run_program_with_pipes()
	{
		log("Starting program execution with pipes...");

		std::string program_path = temp_dir + "/program";
		if (!std::filesystem::exists(program_path))
		{
			log_error("Program not found: " + program_path);
			send_error_message("Program executable not found after compilation", -1);
			return;
		}

		if (access(program_path.c_str(), X_OK) != 0)
		{
			log_error("Program is not executable: " + program_path);
			send_error_message("Program is not executable", -1);
			return;
		}

		log("Program found and executable: " + program_path);

		int inpipe[2], outpipe[2];
		if (pipe(inpipe) < 0 || pipe(outpipe) < 0)
		{
			log_error("Failed to create pipes");
			send_error_message("Failed to create pipes", -1);
			return;
		}

		log("Pipes created successfully");
		pid_t pid = fork();
		if (pid < 0)
		{
			log_error("Fork failed");
			send_error_message("Fork failed", -1);
			return;
		}

		if (pid == 0)
		{
			dup2(inpipe[0], STDIN_FILENO);
			dup2(outpipe[1], STDOUT_FILENO);
			dup2(outpipe[1], STDERR_FILENO);
			close(inpipe[1]);
			close(outpipe[0]);

			struct rlimit rl;
			rl.rlim_cur = rl.rlim_max = (rlim_t)time_limit_sec;
			setrlimit(RLIMIT_CPU, &rl);
			rl.rlim_cur = rl.rlim_max = (rlim_t)memory_limit_mb * 1024 * 1024;
			setrlimit(RLIMIT_AS, &rl);

			if (chdir(temp_dir.c_str()) != 0)
			{
				log_error("Failed to change directory");
				_exit(127);
			}

			std::vector<char *> argv;
			argv.push_back(const_cast<char *>("./program"));
			argv.push_back(nullptr);

			setsid();

			execv("./program", argv.data());

			_exit(127);
		}

		log("Child process created: " + std::to_string(pid));
		close(inpipe[0]);
		close(outpipe[1]);
		int fd_stdout = outpipe[0];
		int fd_stdin = inpipe[1];

		fcntl(fd_stdout, F_SETFL, O_NONBLOCK);
		fcntl(client_fd, F_SETFL, O_NONBLOCK);
		fcntl(fd_stdin, F_SETFL, O_NONBLOCK);

		bool timed_out = false;
		bool memory_exceeded = false;
		std::string program_output;
		std::string current_line;
		auto last_activity_time = steady_clock::now();
		auto program_start = steady_clock::now();
		int consecutive_silence_cycles = 0;
		bool has_output_before_silence = false;

		log("Entering main execution loop...");

		while (true)
		{
			struct pollfd pfds[2];
			pfds[0].fd = fd_stdout;
			pfds[0].events = POLLIN;
			pfds[1].fd = client_fd;
			pfds[1].events = POLLIN;

			int timeout = 100;
			int rc = poll(pfds, 2, timeout);
			auto now = steady_clock::now();

			long elapsed_ms = duration_cast<milliseconds>(now - program_start).count();
			if (elapsed_ms > time_limit_sec * 1000)
			{
				log("Time limit exceeded, killing process...");
				kill(-pid, SIGKILL);
				timed_out = true;
			}

			if (rc > 0)
			{
				if (pfds[0].revents & POLLIN)
				{
					char buf[4096];
					ssize_t r = read(fd_stdout, buf, sizeof(buf));
					if (r > 0)
					{
						std::string chunk(buf, buf + r);
						program_output += chunk;
						last_activity_time = now;
						has_output_before_silence = true;
						consecutive_silence_cycles = 0;

						for (char c : chunk)
						{
							if (c == '\n' || c == '\r')
							{
								current_line.clear();
							}
							else
							{
								current_line += c;
							}
						}

						log("Program output: " + std::to_string(r) + " bytes");
						send_program_output(chunk);

						if (input_sent)
						{
							input_sent = false;
						}
					}
				}

				if (pfds[1].revents & POLLIN)
				{
					char buf[4096];
					ssize_t r = recv(client_fd, buf, sizeof(buf) - 1, 0);
					if (r > 0)
					{
						buf[r] = 0;
						std::string input(buf);
						log("Received input: " + input);

						std::string tosend = input + "\n";
						write(fd_stdin, tosend.c_str(), tosend.size());
						input_sent = true;
						waiting_for_input = false;
						last_activity_time = now;
						consecutive_silence_cycles = 0;
					}
					else if (r == 0)
					{
						log("Client disconnected during input");
						send_error_message("Client disconnected during input", -6);
						break;
					}
				}
			}

			if (!waiting_for_input && !input_sent && has_output_before_silence)
			{
				auto silence_duration = duration_cast<milliseconds>(now - last_activity_time);

				if (silence_duration.count() > 200)
				{
					consecutive_silence_cycles++;

					bool should_request_input = false;
					std::string prompt = "Enter input:";

					if (!current_line.empty())
					{
						std::string line_lower = current_line;
						std::transform(line_lower.begin(), line_lower.end(), line_lower.begin(), ::tolower);

						bool has_explicit_prompt =
								current_line.back() == ':' ||
								current_line.back() == '>' ||
								line_lower.find("enter") != std::string::npos ||
								line_lower.find("input") != std::string::npos;

						if (has_explicit_prompt && consecutive_silence_cycles >= 2)
						{
							should_request_input = true;
							prompt = "Program expects input: " + current_line;
						}
					}

					if (!should_request_input && consecutive_silence_cycles >= 3)
					{
						should_request_input = true;
						prompt = "Program is waiting for input...";
					}

					if (should_request_input)
					{
						log("Detected input request. Silence: " + std::to_string(silence_duration.count()) +
								"ms, Cycles: " + std::to_string(consecutive_silence_cycles));
						send_input_request(prompt);
						current_line.clear();
						consecutive_silence_cycles = 0;
					}
				}
			}

			int status;
			pid_t w = waitpid(pid, &status, WNOHANG);
			if (w == pid)
			{
				int exit_code = WIFEXITED(status) ? WEXITSTATUS(status) : -1;
				log("Process completed with exit code: " + std::to_string(exit_code));
				close(fd_stdout);
				close(fd_stdin);
				send_json_result(exit_code, program_output, "", timed_out, memory_exceeded);
				break;
			}

			if (timed_out)
			{
				log("Process timed out");
				waitpid(pid, NULL, 0);
				close(fd_stdout);
				close(fd_stdin);
				send_json_result(-4, program_output, "Time limit exceeded", true, false);
				break;
			}

			std::this_thread::sleep_for(std::chrono::milliseconds(10));
		}

		log("Execution loop finished");
	}
};

InteractiveSandbox::InteractiveSandbox(const std::string &dir, int tlim, int mlim)
{
	pimpl = new InteractiveSandboxImpl(dir, tlim, mlim);
}

InteractiveSandbox::~InteractiveSandbox()
{
	delete pimpl;
}

void InteractiveSandbox::cleanup()
{
	pimpl->cleanup();
}

void InteractiveSandbox::execute_with_pipes(const std::string &code, int sockfd)
{
	pimpl->execute_with_pipes(code, sockfd);
}

void InteractiveSandbox::execute_multiple_files_with_pipes(const std::map<std::string, std::string> &files, int sockfd)
{
	pimpl->execute_multiple_files_with_pipes(files, sockfd);
}

Sandbox::Sandbox(const std::string &temp_dir_in) : temp_dir(temp_dir_in), time_limit_sec(5), memory_limit_mb(256)
{
	std::filesystem::create_directories(temp_dir);
}

Sandbox::~Sandbox() { cleanup(); }

void Sandbox::cleanup()
{
	try
	{
		if (std::filesystem::exists(temp_dir))
			std::filesystem::remove_all(temp_dir);
	}
	catch (...)
	{
	}
}

bool Sandbox::create_temp_environment()
{
	try
	{
		auto unique_dir = temp_dir + "/run_" + std::to_string(getpid()) + "_" + std::to_string(time(nullptr));
		std::filesystem::create_directories(unique_dir);
		temp_dir = unique_dir;
		return true;
	}
	catch (...)
	{
		return false;
	}
}

bool Sandbox::is_dangerous_code(const std::string &code)
{
	std::vector<std::string> dangerous_patterns = {"system(", "popen(", "exec(", "fork(", "rm -rf", "chmod", "shutdown"};
	std::string code_lower = code;
	std::transform(code_lower.begin(), code_lower.end(), code_lower.begin(), ::tolower);
	for (const auto &pattern : dangerous_patterns)
		if (code_lower.find(pattern) != std::string::npos)
			return true;
	return false;
}

bool Sandbox::compile_code(const std::string &code, std::string &error)
{
	std::ofstream source_file(temp_dir + "/program.cpp");
	if (!source_file)
	{
		error = "Cannot create source file";
		return false;
	}
	source_file << code;
	source_file.close();
	std::string command = "cd \"" + temp_dir + "\" && g++ -std=c++17 -O2 program.cpp -o program 2> compile_errors.txt";
	int compile_status = system(command.c_str());
	std::ifstream error_file(temp_dir + "/compile_errors.txt");
	if (error_file)
	{
		std::string line;
		while (std::getline(error_file, line))
			error += line + "\n";
		error_file.close();
	}
	bool success = (compile_status == 0) && std::filesystem::exists(temp_dir + "/program");
	if (success)
	{
		chmod((temp_dir + "/program").c_str(), 0755);
	}
	return success;
}

ExecutionResult Sandbox::run_program(const std::string &input)
{
	ExecutionResult result;
	result.timed_out = false;
	result.memory_exceeded = false;
	result.memory_used_kb = 0;

	std::ofstream input_file(temp_dir + "/input.txt");
	if (input_file)
	{
		input_file << input;
		input_file.close();
	}

	int inpipe[2], outpipe[2];
	if (pipe(inpipe) < 0 || pipe(outpipe) < 0)
	{
		result.exit_code = -1;
		result.error = "Failed to create pipes";
		result.status = "PROCESS_ERROR";
		return result;
	}

	pid_t pid = fork();
	auto start = steady_clock::now();
	if (pid < 0)
	{
		result.exit_code = -1;
		result.error = "Fork failed";
		result.status = "PROCESS_ERROR";
		return result;
	}

	if (pid == 0)
	{
		dup2(inpipe[0], STDIN_FILENO);
		dup2(outpipe[1], STDOUT_FILENO);
		dup2(outpipe[1], STDERR_FILENO);
		close(inpipe[1]);
		close(outpipe[0]);

		struct rlimit rl;
		rl.rlim_cur = rl.rlim_max = (rlim_t)time_limit_sec;
		setrlimit(RLIMIT_CPU, &rl);
		rl.rlim_cur = rl.rlim_max = (rlim_t)memory_limit_mb * 1024 * 1024;
		setrlimit(RLIMIT_AS, &rl);

		chdir(temp_dir.c_str());
		execl("./program", "program", (char *)NULL);
		_exit(127);
	}

	close(inpipe[0]);
	close(outpipe[1]);
	int fd_out = outpipe[0];
	int fd_in = inpipe[1];
	fcntl(fd_out, F_SETFL, O_NONBLOCK);

	bool timed_out = false;
	std::string output;

	while (true)
	{
		char buf[4096];
		ssize_t r = read(fd_out, buf, sizeof(buf));
		if (r > 0)
			output.append(buf, buf + r);

		int status;
		pid_t w = waitpid(pid, &status, WNOHANG);
		auto now = steady_clock::now();

		if (duration_cast<milliseconds>(now - start).count() > time_limit_sec * 1000)
		{
			kill(-pid, SIGKILL);
			timed_out = true;
		}

		if (w == pid)
		{
			result.exit_code = WIFEXITED(status) ? WEXITSTATUS(status) : -1;
			break;
		}

		if (timed_out)
		{
			waitpid(pid, NULL, 0);
			result.exit_code = -4;
			result.timed_out = true;
			result.status = "TIME_LIMIT_EXCEEDED";
			break;
		}

		std::this_thread::sleep_for(std::chrono::milliseconds(10));
	}

	close(fd_out);
	close(fd_in);

	std::ifstream outf(temp_dir + "/output.txt");
	if (outf)
	{
		std::ostringstream ss;
		ss << outf.rdbuf();
		result.output = ss.str();
		outf.close();
	}
	else
	{
		result.output = output;
	}

	std::ifstream errf(temp_dir + "/runtime_errors.txt");
	if (errf)
	{
		std::ostringstream se;
		se << errf.rdbuf();
		result.error = se.str();
		errf.close();
	}

	if (result.status.empty())
	{
		if (result.exit_code == 0)
			result.status = "SUCCESS";
		else
			result.status = "RUNTIME_ERROR";
	}

	auto end = steady_clock::now();
	result.execution_time_ms = duration_cast<milliseconds>(end - start).count();
	return result;
}

ExecutionResult Sandbox::execute(const std::string &code, const std::string &input, int tlim, int mlim)
{
	this->time_limit_sec = tlim;
	this->memory_limit_mb = mlim;
	ExecutionResult res;

	if (is_dangerous_code(code))
	{
		res.error = "Dangerous code detected: execution blocked";
		res.exit_code = -3;
		res.status = "SECURITY_ERROR";
		return res;
	}

	if (!create_temp_environment())
	{
		res.error = "Failed to create temp environment";
		res.exit_code = 1;
		res.status = "SANDBOX_ERROR";
		return res;
	}

	std::string compile_error;
	if (!compile_code(code, compile_error))
	{
		res.error = "Compilation failed: " + compile_error;
		res.exit_code = -2;
		res.status = "COMPILATION_ERROR";
		return res;
	}

	res = run_program(input);
	return res;
}