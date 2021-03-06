package net.toastynetworks.beaconwatch;

import java.sql.Types;

import net.kaikk.mc.kaiscommons.sql.SQLConnection;
import net.kaikk.mc.kaiscommons.sql.SQLConnection.PreparedCachedStatement;

public class SQLStatements {
	PreparedCachedStatement createTablePlayers, createTableGame, createTablePlayerStatistics, createTableOnlinePlayers, insertPlayer, updatePlayer, insertGame, updateGame, insertOnlinePlayer, deleteOnlinePlayer, truncateOnlinePlayer, insertPlayerStatistics, updatePlayerStatistics, selectPlayerStats, createTableFaction;
	
	public SQLConnection connection;
	public SQLStatements(SQLConnection connection) {
		this.connection = connection;
		this.createTablePlayers = connection.prepareCachedStatement("CREATE TABLE IF NOT EXISTS players (uuid CHAR(36) NOT NULL, name CHAR(16) NOT NULL, first_login TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, last_seen TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, time_played INT NOT NULL DEFAULT 0, factionid INT NOT NULL DEFAULT 0, PRIMARY KEY (uuid))");
		this.createTableGame = connection.prepareCachedStatement("CREATE TABLE IF NOT EXISTS game (gameid INT NOT NULL AUTO_INCREMENT, time_game_starts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, time_game_ends TIMESTAMP NOT NULL DEFAULT 0 , winning_team TINYINT NOT NULL, PRIMARY KEY (gameid))");
		this.createTablePlayerStatistics = connection.prepareCachedStatement("CREATE TABLE IF NOT EXISTS playerstatistics (uuid CHAR(36) NOT NULL, gameid INT NOT NULL, beacons_destroyed INT NOT NULL DEFAULT 0, kills INT NOT NULL DEFAULT 0, deaths INT NOT NULL DEFAULT 0, blocks_broken INT NOT NULL DEFAULT 0, blocks_placed INT NOT NULL DEFAULT 0, team TINYINT UNSIGNED NOT NULL, PRIMARY KEY (gameid, uuid), KEY uuid (uuid))");
		this.createTableOnlinePlayers = connection.prepareCachedStatement("CREATE TABLE IF NOT EXISTS onlineplayers (uuid CHAR(36) NOT NULL, PRIMARY KEY (uuid))");
		this.createTableFaction = connection.prepareCachedStatement("CREATE TABLE IF NOT EXISTS faction (factionid INTEGER NOT NULL AUTO_INCREMENT, name CHAR(16) NOT NULL, PRIMARY KEY (factionid))");
		this.insertPlayer = connection.prepareCachedStatement("INSERT IGNORE INTO players (uuid, name) VALUES (?, ?)", Types.CHAR, Types.CHAR);
		this.updatePlayer = connection.prepareCachedStatement("UPDATE players SET last_seen = CURRENT_TIMESTAMP, time_played = time_played + ? WHERE uuid = ?", Types.INTEGER, Types.CHAR);
		this.insertGame = connection.prepareCachedStatement("INSERT INTO game (winning_team) VALUES (-1)", true);
		this.updateGame = connection.prepareCachedStatement("UPDATE game SET time_game_ends = CURRENT_TIMESTAMP, winning_team = ? WHERE gameid = ?", Types.TINYINT, Types.INTEGER);
		this.insertOnlinePlayer = connection.prepareCachedStatement("INSERT IGNORE INTO onlineplayers (uuid) VALUES (?)", Types.CHAR);
		this.deleteOnlinePlayer = connection.prepareCachedStatement("DELETE FROM onlineplayers WHERE uuid = ?", Types.CHAR);
		this.truncateOnlinePlayer = connection.prepareCachedStatement("TRUNCATE TABLE onlineplayers");
		this.insertPlayerStatistics = connection.prepareCachedStatement("INSERT INTO playerstatistics (uuid, gameid, team) VALUES(?, ?, ?)",Types.CHAR, Types.INTEGER, Types.TINYINT);
		this.updatePlayerStatistics = connection.prepareCachedStatement("UPDATE playerstatistics SET beacons_destroyed = ?, kills = ?, deaths = ?, blocks_broken = ?, blocks_placed = ? WHERE gameid = ? AND uuid = ?", Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.CHAR);
		this.selectPlayerStats = connection.prepareCachedStatement("SELECT p.time_played, p.first_login, p.last_seen, SUM(s.beacons_destroyed), SUM(s.kills), SUM(s.deaths), SUM(s.blocks_broken), SUM(s.blocks_placed) FROM players AS p INNER JOIN playerstatistics AS s ON p.uuid = s.uuid WHERE p.uuid = ?", Types.CHAR);
		
		/*
		 * Statistics commands:
		 * - Playerstats: current player: time_played, kills, deaths, beacons destroyed, blocks placed, blocks broken, last_seen, first_login
		 * - Gamestats: current game: kills, deaths, beacons destroyed, blocks placed, blocks broken ------------ OPTIONAL
		 * - Serverstats: all time, total statistics: time_played, kills, deaths, beacons destroyed, blocks placed, blocks broken, total games played
		 * */
		
		/*
		 * Sub-Query
		 * INNER JOIN
		 * GROUP BY / HAVING
		 * FUNCTIONS ( COUNT / SUM )
		 * */
	}
}
