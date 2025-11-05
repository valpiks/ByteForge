#ifndef SOCKETSERVER_H
#define SOCKETSERVER_H

#include <string>
#include <thread>
#include <atomic>

class SocketServer
{
private:
	int port;
	std::atomic<bool> running;
	std::thread server_thread;

public:
	SocketServer(int port);
	~SocketServer();
	void start();
	void stop();

private:
	void run_server();
	void handle_client(int client_socket);
	void log(const std::string &message);
	void log_error(const std::string &message);
};

std::string unescape_json_string(const std::string &input);

#endif