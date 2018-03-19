package com.swift;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginUsersLogTableRepository extends JpaRepository<LoginUsersLogTable, Integer> {


	public List<LoginUsersLogTable> findByUserIdAndImeiAndLogin(String userId, String imei, Boolean login);

	public List<LoginUsersLogTable> findByUserIdAndSPassAndLogin(String userId, String sPass, Boolean login);

	public List<LoginUsersLogTable> findByLoginAndUserId(Boolean login, String userId);

	public List<LoginUsersLogTable> findByLoginAndImei(Boolean login, String imei);

	public List<LoginUsersLogTable> findByLoginAndSPass(Boolean login, String sPass);

}
