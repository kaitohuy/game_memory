-- SQL schema additions for Solo and Matches

-- Solo sessions and rounds
CREATE TABLE IF NOT EXISTS solo_sessions (
  sessionId INT AUTO_INCREMENT PRIMARY KEY,
  userId INT NOT NULL,
  startTime DATETIME DEFAULT CURRENT_TIMESTAMP,
  endTime DATETIME DEFAULT NULL,
  totalScore INT DEFAULT 0,
  roundsPlayed INT DEFAULT 0,
  FOREIGN KEY (userId) REFERENCES users(userId)
);

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
  FOREIGN KEY (sessionId) REFERENCES solo_sessions(sessionId)
);

-- Matches and match rounds
CREATE TABLE IF NOT EXISTS matches (
  matchId INT AUTO_INCREMENT PRIMARY KEY,
  playerA INT NOT NULL,
  playerB INT NOT NULL,
  startTime DATETIME DEFAULT CURRENT_TIMESTAMP,
  endTime DATETIME DEFAULT NULL,
  winner INT DEFAULT NULL,
  isActive BOOLEAN DEFAULT TRUE,
  FOREIGN KEY (playerA) REFERENCES users(userId),
  FOREIGN KEY (playerB) REFERENCES users(userId)
);

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
  FOREIGN KEY (matchId) REFERENCES matches(matchId)
);
