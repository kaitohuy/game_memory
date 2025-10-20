-- Migration: create leaderboards for competitive and solo
-- Competitive: name, wins, points
-- Solo: name, roundsWon, points

CREATE TABLE IF NOT EXISTS leaderboard_competitive (
    id INT AUTO_INCREMENT PRIMARY KEY,
    playerName VARCHAR(100) NOT NULL,
    wins INT NOT NULL DEFAULT 0,
    points INT NOT NULL DEFAULT 0,
    lastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_player_competitive (playerName)
);

CREATE TABLE IF NOT EXISTS leaderboard_solo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    playerName VARCHAR(100) NOT NULL,
    roundsWon INT NOT NULL DEFAULT 0,
    points INT NOT NULL DEFAULT 0,
    lastUpdated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_player_solo (playerName)
);
