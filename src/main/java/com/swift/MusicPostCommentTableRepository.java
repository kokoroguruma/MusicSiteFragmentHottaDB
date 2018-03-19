package com.swift;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MusicPostCommentTableRepository extends JpaRepository<MusicPostCommentTable, Integer> {


	public List<MusicPostCommentTable> findByMusicId(Integer musicId);


}
