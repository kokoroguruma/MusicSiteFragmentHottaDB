package com.swift;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicUrlTableRepository extends JpaRepository<MusicUrlTable, Integer> {


	public List<MusicUrlTable> findByMusicNameLikeOrMusicCommentLike(String musicName, String musicComment);

	public List<MusicUrlTable> findByMusicNameLike(String musicName);

	public List<MusicUrlTable> findByMusicName(String musicName);



}
