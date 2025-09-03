#include <iostream>
#include <memory>
#include "Application.h"

/**
 * @brief Main entry point of the application
 * 
 * This function creates and runs the main application instance,
 * handling any exceptions that might occur during execution.
 */
int main() {
    try {
        // Create application instance with default parameters
        auto app = std::make_unique<Application>();

        // Initialize the application
        if (!app->initialize()) {
            std::cerr << "Failed to initialize application" << std::endl;
            return 1;
        }
        
        // Run the main application loop
        const int exitCode = app->run();
        
        // Shutdown gracefully
        app->shutdown();
        
        return exitCode;
        
    } catch (const std::exception& e) {
        std::cerr << "Fatal error: " << e.what() << std::endl;
        return 1;
    } catch (...) {
        std::cerr << "Unknown fatal error occurred" << std::endl;
        return 1;
    }
}


