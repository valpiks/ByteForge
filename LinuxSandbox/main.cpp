#include "src/socketserver.h"
#include <iostream>
#include <csignal>
#include <atomic>

std::atomic<bool> running{true};

void signal_handler(int signal)
{
	running = false;
}

int main()
{
	std::cout << "Starting C++ Sandbox Server on port 8884..." << std::endl;

	std::signal(SIGINT, signal_handler);
	std::signal(SIGTERM, signal_handler);

	SocketServer server(8884);
	server.start();

	std::cout << "Server is running. Press Ctrl+C to stop..." << std::endl;

	while (running)
	{
		std::this_thread::sleep_for(std::chrono::milliseconds(100));
	}

	std::cout << "Shutting down server..." << std::endl;
	server.stop();

	std::cout << "Server stopped successfully." << std::endl;
	return 0;
}