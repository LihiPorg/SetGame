# Set Card Game - Java Concurrency and Synchronization

This project is an implementation of a simplified version of the Set card game, developed as part of a concurrency and synchronization assignment in Java. The focus of the project is on managing multiple threads and ensuring synchronized operations within the game logic.

## Project Overview

- **Game Description**: The Set card game is a puzzle game where players try to identify sets of three cards from a grid, where each card in a set either shares all the same features or has completely different features from the others.
- **Concurrency**: The game simulates multiple players (both human and computer) interacting with the game table simultaneously, requiring careful management of threads and synchronization.
- **Objective**: Players aim to identify and collect sets of three cards. The player with the most sets at the end of the game wins.

## Features

- **Multi-threaded Gameplay**: Each player, including non-human players, operates on its own thread.
- **Synchronization**: Ensures that players interact with the game table without conflicts.
- **Customizable Gameplay**: The game configuration can be modified via a `config.properties` file.
- **Automatic and Manual Players**: The game supports both human players via keyboard input and non-human players simulated by the system.

## Project Structure

- **Main Components**:
  - `Dealer.java`: Manages the game flow, dealing cards, and checking for sets.
  - `Player.java`: Represents a player in the game, handling player actions.
  - `Table.java`: Represents the game table, holding cards and tracking player actions.
  - `UserInterfaceImpl.java`: Manages the game's graphical user interface (GUI).
  
- **Configuration**:
  - The game behavior can be customized by editing the `config.properties` file, including the number of players, timeouts, and more.

## Setup and Installation

### Prerequisites

- **Java 11 or higher**
- **Maven** for building the project

### Installation

1. **Clone the Repository**

2. **Build the Project**:
   ```bash
   mvn clean install
   ```

### Running the Game

1. **Compile the Project**:
   ```bash
   mvn compile
   ```

2. **Run the Game**:
   ```bash
   mvn exec:java
   ```

## How to Play

- **Controls**: Each player controls a set of keys corresponding to positions on the game table.
- **Objective**: Identify and claim sets of three cards that meet the game's criteria.
- **Winning**: The game ends when no more sets are available, and the player with the most sets wins.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
