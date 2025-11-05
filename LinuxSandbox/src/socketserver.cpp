#include "socketserver.h"
#include "sandbox.h"
#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <thread>
#include <iostream>
#include <atomic>
#include <map>
#include <string>
#include <cstring>

SocketServer::SocketServer(int port) : port(port), running(false) {}
SocketServer::~SocketServer() { stop(); }

void SocketServer::start()
{
	if (!running)
	{
		running = true;
		server_thread = std::thread(&SocketServer::run_server, this);
	}
}

void SocketServer::stop()
{
	if (running)
	{
		running = false;
		if (server_thread.joinable())
		{
			server_thread.join();
		}
	}
}

void SocketServer::run_server()
{
	int server_socket = socket(AF_INET, SOCK_STREAM, 0);
	if (server_socket < 0)
	{
		log_error("Socket creation failed");
		return;
	}

	int opt = 1;
	if (setsockopt(server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0)
	{
		log_error("Setsockopt failed");
		close(server_socket);
		return;
	}

	sockaddr_in server_addr;
	server_addr.sin_family = AF_INET;
	server_addr.sin_addr.s_addr = INADDR_ANY;
	server_addr.sin_port = htons(port);

	if (bind(server_socket, (sockaddr *)&server_addr, sizeof(server_addr)) < 0)
	{
		log_error("Bind failed on port " + std::to_string(port));
		close(server_socket);
		return;
	}

	if (listen(server_socket, 10) < 0)
	{
		log_error("Listen failed");
		close(server_socket);
		return;
	}

	log("Socket server started on port " + std::to_string(port));
	log("Ready to accept connections...");

	while (running)
	{
		int client_socket = accept(server_socket, NULL, NULL);
		if (client_socket >= 0)
		{
			log("New client connected");
			std::thread client_thread(&SocketServer::handle_client, this, client_socket);
			client_thread.detach();
		}
		else
		{
			if (running)
			{
				log_error("Accept failed");
			}
		}
	}

	log("Server shutting down...");
	close(server_socket);
}

std::string unescape_json_string(const std::string &input)
{
	std::string output;
	for (size_t i = 0; i < input.length(); ++i)
	{
		if (input[i] == '\\' && i + 1 < input.length())
		{
			switch (input[i + 1])
			{
			case 'n':
				output += '\n';
				i++;
				break;
			case 't':
				output += '\t';
				i++;
				break;
			case 'r':
				output += '\r';
				i++;
				break;
			case '"':
				output += '"';
				i++;
				break;
			case '\\':
				output += '\\';
				i++;
				break;
			case '/':
				output += '/';
				i++;
				break;
			case 'b':
				output += '\b';
				i++;
				break;
			case 'f':
				output += '\f';
				i++;
				break;
			case 'u':
				if (i + 5 < input.length())
				{
					output += input.substr(i, 6);
					i += 5;
				}
				else
				{
					output += input[i];
				}
				break;
			default:
				output += input[i];
				break;
			}
		}
		else
		{
			output += input[i];
		}
	}
	return output;
}

void SocketServer::handle_client(int client_socket)
{
	log("Handling new client connection");

	char code_buffer[65536];
	int code_size = recv(client_socket, code_buffer, sizeof(code_buffer) - 1, 0);

	if (code_size > 0)
	{
		code_buffer[code_size] = 0;
		std::string received_data(code_buffer);

		std::string code;
		std::map<std::string, std::string> files;
		int time_limit = 5;
		int memory_limit = 256;
		bool is_multi_file = false;

		log("Raw received data (first 500 chars): " + received_data.substr(0, std::min(500, (int)received_data.length())) + "...");

		if (received_data[0] == '{')
		{
			try
			{
				size_t files_pos = received_data.find("\"files\":");
				if (files_pos != std::string::npos)
				{
					is_multi_file = true;

					size_t files_start = received_data.find('{', files_pos);
					size_t files_end = files_start;
					int brace_count = 0;

					for (size_t i = files_start; i < received_data.length(); i++)
					{
						if (received_data[i] == '{')
							brace_count++;
						else if (received_data[i] == '}')
							brace_count--;

						if (brace_count == 0)
						{
							files_end = i;
							break;
						}
					}

					if (files_end > files_start)
					{
						std::string files_json = received_data.substr(files_start, files_end - files_start + 1);
						log("Files JSON: " + files_json.substr(0, std::min(200, (int)files_json.length())) + "...");

						size_t pos = files_start + 1;
						while (pos < files_end)
						{
							size_t key_start = received_data.find('"', pos);
							if (key_start == std::string::npos || key_start >= files_end)
								break;

							size_t key_end = received_data.find('"', key_start + 1);
							if (key_end == std::string::npos || key_end >= files_end)
								break;

							std::string filename = received_data.substr(key_start + 1, key_end - key_start - 1);

							size_t value_start = received_data.find('"', key_end + 1);
							if (value_start == std::string::npos || value_start >= files_end)
								break;

							size_t value_end = value_start + 1;
							bool in_escape = false;

							while (value_end < files_end)
							{
								if (in_escape)
								{
									in_escape = false;
								}
								else if (received_data[value_end] == '\\')
								{
									in_escape = true;
								}
								else if (received_data[value_end] == '"')
								{
									break;
								}
								value_end++;
							}

							if (value_end >= files_end)
								break;

							std::string file_content = received_data.substr(value_start + 1, value_end - value_start - 1);
							file_content = unescape_json_string(file_content);

							files[filename] = file_content;
							log("Found filename: " + filename);
							log("File content length: " + std::to_string(file_content.length()));

							pos = value_end + 1;
						}
					}
				}
				else
				{
					size_t code_pos = received_data.find("\"code\":");
					if (code_pos != std::string::npos)
					{
						size_t value_start = received_data.find('"', code_pos + 7);
						if (value_start != std::string::npos)
						{
							size_t value_end = value_start + 1;
							bool in_escape = false;

							while (value_end < received_data.length())
							{
								if (in_escape)
								{
									in_escape = false;
								}
								else if (received_data[value_end] == '\\')
								{
									in_escape = true;
								}
								else if (received_data[value_end] == '"')
								{
									break;
								}
								value_end++;
							}

							if (value_end < received_data.length())
							{
								code = received_data.substr(value_start + 1, value_end - value_start - 1);
								code = unescape_json_string(code);
							}
						}
					}
				}

				size_t time_pos = received_data.find("\"time_limit\":");
				if (time_pos == std::string::npos)
					time_pos = received_data.find("\"timeLimitSec\":");

				if (time_pos != std::string::npos)
				{
					size_t colon_pos = received_data.find(':', time_pos);
					if (colon_pos != std::string::npos)
					{
						size_t value_start = received_data.find_first_of("0123456789", colon_pos);
						size_t value_end = received_data.find_first_not_of("0123456789", value_start);
						if (value_end == std::string::npos)
							value_end = received_data.length();

						std::string time_str = received_data.substr(value_start, value_end - value_start);
						time_limit = std::stoi(time_str);
					}
				}

				size_t memory_pos = received_data.find("\"memory_limit\":");
				if (memory_pos == std::string::npos)
					memory_pos = received_data.find("\"memoryLimitMb\":");

				if (memory_pos != std::string::npos)
				{
					size_t colon_pos = received_data.find(':', memory_pos);
					if (colon_pos != std::string::npos)
					{
						size_t value_start = received_data.find_first_of("0123456789", colon_pos);
						size_t value_end = received_data.find_first_not_of("0123456789", value_start);
						if (value_end == std::string::npos)
							value_end = received_data.length();

						std::string memory_str = received_data.substr(value_start, value_end - value_start);
						memory_limit = std::stoi(memory_str);
					}
				}
			}
			catch (const std::exception &e)
			{
				log_error("JSON parsing error: " + std::string(e.what()));
			}
		}
		else
		{
			code = received_data;
		}

		log("Received code with limits - Time: " + std::to_string(time_limit) +
				"s, Memory: " + std::to_string(memory_limit) + "MB");
		log("Multi-file mode: " + std::string(is_multi_file ? "YES" : "NO"));
		log("Files count: " + std::to_string(files.size()));

		InteractiveSandbox sandbox("./tmp/socket_sandbox_" + std::to_string(getpid()),
															 time_limit, memory_limit);

		if (is_multi_file && !files.empty())
		{
			log("Executing multi-file project with " + std::to_string(files.size()) + " files");
			sandbox.execute_multiple_files_with_pipes(files, client_socket);
		}
		else
		{
			log("Executing single file");
			sandbox.execute_with_pipes(code, client_socket);
		}
	}
	else if (code_size == 0)
	{
		log("Client disconnected before sending code");
	}
	else
	{
		log_error("recv failed");
	}

	close(client_socket);
	log("Client handling completed");
}

void SocketServer::log(const std::string &message)
{
	auto now = std::chrono::system_clock::now();
	auto time_t = std::chrono::system_clock::to_time_t(now);
	char time_str[9];
	std::strftime(time_str, sizeof(time_str), "%H:%M:%S", std::localtime(&time_t));
	std::cout << "[SERVER][" << time_str << "] " << message << std::endl;
}

void SocketServer::log_error(const std::string &message)
{
	auto now = std::chrono::system_clock::now();
	auto time_t = std::chrono::system_clock::to_time_t(now);
	char time_str[9];
	std::strftime(time_str, sizeof(time_str), "%H:%M:%S", std::localtime(&time_t));
	std::cerr << "[SERVER][" << time_str << "] ❌ " << message << std::endl;
}