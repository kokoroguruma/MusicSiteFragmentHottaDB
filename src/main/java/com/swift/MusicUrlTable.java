package com.swift;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="music_url")
public class MusicUrlTable {


	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="music_id")
	private Integer musicId;

	@Column(name="music_name")
	private String musicName;

	@Column(name="music_url")
	private String musicUrl;

	@Column(name="music_comment")
	private String musicComment;






	public Integer getMusicId() {
		return musicId;
	}

	public void setMusicId(Integer musicId) {
		this.musicId = musicId;
	}

	public String getMusicName() {
		return musicName;
	}

	public void setMusicName(String musicName) {
		this.musicName = musicName;
	}

	public String getMusicUrl() {
		return musicUrl;
	}

	public void setMusicUrl(String musicUrl) {
		this.musicUrl = musicUrl;
	}

	public String getMusicComment() {
		return musicComment;
	}

	public void setMusicComment(String musicComment) {
		this.musicComment = musicComment;
	}



}
