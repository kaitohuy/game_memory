-- Full database schema for Memory Game application
-- Run this script on your MySQL server (e.g., using MySQL Workbench or mysql CLI)

CREATE DATABASE IF NOT EXISTS game_memory DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE game_memory;

-- users table
CREATE TABLE IF NOT EXISTS users (
  userId INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  passwordHash VARCHAR(255) NOT NULL,
  totalScore INT DEFAULT 0,
  totalWins INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'OFFLINE',
  createdAt DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Solo sessions and rounds
CREATE TABLE IF NOT EXISTS solo_sessions (
  sessionId INT AUTO_INCREMENT PRIMARY KEY,
  userId INT NOT NULL,
  startTime DATETIME DEFAULT CURRENT_TIMESTAMP,
  endTime DATETIME DEFAULT NULL,
  totalScore INT DEFAULT 0,
  roundsPlayed INT DEFAULT 0,
  FOREIGN KEY (userId) REFERENCES users(userId) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS solo_rounds (
  roundId INT AUTO_INCREMENT PRIMARY KEY,
  sessionId INT NOT NULL,
  roundNumber INT NOT NULL,
  displayedText VARCHAR(255) NOT NULL,
  length INT,
  ttlSeconds INT,
  userAnswer VARCHAR(255),
  correct BOOLEAN,
  timeTakenMillis INT,
  createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (sessionId) REFERENCES solo_sessions(sessionId) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Matches and match rounds
CREATE TABLE IF NOT EXISTS matches (
  matchId INT AUTO_INCREMENT PRIMARY KEY,
  playerA INT NOT NULL,
  playerB INT NOT NULL,
  startTime DATETIME DEFAULT CURRENT_TIMESTAMP,
  endTime DATETIME DEFAULT NULL,
  winner INT DEFAULT NULL,
  isActive BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (playerA) REFERENCES users(userId) ON DELETE CASCADE,
  FOREIGN KEY (playerB) REFERENCES users(userId) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS match_rounds (
  id INT AUTO_INCREMENT PRIMARY KEY,
  matchId INT NOT NULL,
  roundNumber INT NOT NULL,
  displayedText VARCHAR(255) NOT NULL,
  length INT,
  ttlSeconds INT,
  playerA_answer VARCHAR(255),
  playerA_timeMillis INT,
  playerB_answer VARCHAR(255),
  playerB_timeMillis INT,
  playerA_points INT DEFAULT 0,
  playerB_points INT DEFAULT 0,
  createdAt DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (matchId) REFERENCES matches(matchId) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Indexes to speed up leaderboard queries
CREATE INDEX idx_users_score ON users(totalScore DESC, totalWins DESC);

-- Leaderboard view (optional)
DROP VIEW IF EXISTS leaderboard_view;
CREATE VIEW leaderboard_view AS
SELECT userId, username, totalScore, totalWins
FROM users
ORDER BY totalScore DESC, totalWins DESC;

-- Sample data
INSERT INTO users (username, passwordHash, totalScore, totalWins, status) VALUES
('alice', 'e3afed0047b08059d0fada10f400c1e5', 120, 3, 'OFFLINE'), -- 'password' md5 placeholder
('bob', '5f4dcc3b5aa765d61d8327deb882cf99', 90, 1, 'OFFLINE'),   -- 'password' md5 placeholder
('carol', '202cb962ac59075b964b07152d234b70', 60, 0, 'OFFLINE');

-- Example: create a match record and a round (for demonstration)
INSERT INTO matches (playerA, playerB, isActive) VALUES (1, 2, FALSE);
INSERT INTO match_rounds (matchId, roundNumber, displayedText, length, ttlSeconds, playerA_answer, playerA_timeMillis, playerB_answer, playerB_timeMillis, playerA_points, playerB_points)
VALUES (LAST_INSERT_ID(), 1, 'ABCD', 4, 15, 'ABCD', 5000, 'ABCD', 6000, 2, 1);

-- Example: create a solo session
INSERT INTO solo_sessions (userId, totalScore, roundsPlayed) VALUES (3, 10, 2);
INSERT INTO solo_rounds (sessionId, roundNumber, displayedText, length, ttlSeconds, userAnswer, correct, timeTakenMillis)
VALUES (LAST_INSERT_ID(), 1, '12AB', 4, 3, '12AB', TRUE, 2000);

-- Notes:
-- 1) The passwordHash values above are placeholders; registration in server will store SHA-256 hashes.
-- 2) Adjust `ConnectionSQL.URL`, `USER`, `PASSWORD` in `ConnectionSQL.java` to connect to your MySQL server.

