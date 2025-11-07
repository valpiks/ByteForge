#ifndef SANDBOX_H
#define SANDBOX_H

#include <string>
#include <map>
#include <atomic>
#include <chrono>

struct ExecutionResult
{
	std::string output;
	std::string error;
	std::string status;
	int exit_code;
	long execution_time_ms;
	unsigned long memory_used_kb;
	bool timed_out;
	bool memory_exceeded;
};

class InteractiveSandbox
{
private:
	class InteractiveSandboxImpl;
	InteractiveSandboxImpl *pimpl;

public:
	InteractiveSandbox(const std::string &dir, int time_limit = 5, int memory_limit = 256);
	~InteractiveSandbox();

	void cleanup();
	void execute_with_pipes(const std::string &code, int sockfd);
	void execute_multiple_files_with_pipes(const std::map<std::string, std::string> &files, int sockfd);
};

std::string escape_json_str(const std::string &in);

#endif