package com.swift;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HottaRestController {

	@Autowired
	private UsersTableRepository usersTableRepository;

	@Autowired
	private LoginUsersLogTableRepository loginUsersLogTableRepository;

	@Autowired
	private MusicUrlTableRepository musicUrlTableRepository;

	@Autowired
	private MusicPostCommentTableRepository musicPostCommentTableRepository;

	/**
	 * ユーザーのログインチェック
	 * ログイン時はsPassをUUIDで生成する。
	 * sucsess: "1"/"0"
	 * s_pass: "s_passの内容"/""
	 * @param userId
	 * @param userPass
	 * @param imei
	 * @return JSON型
	 */
	@RequestMapping(value = "/loginCheck", method = RequestMethod.GET)
	public Map<String, Object> loginCheck(
			@RequestParam(name = "userId", defaultValue = "") String userId,
			@RequestParam(name = "userPass", defaultValue = "") String userPass,
			@RequestParam(name = "imei", defaultValue = "") String imei) {

		Map<String, Object> resultMap = new HashMap<String, Object>();

		resultMap.put("sucsess", "0");
		resultMap.put("s_pass", "");

		if (userId.length() == 0 || userPass.length() == 0 || imei.length() == 0) {
			return resultMap;
		}

		UsersTable user = usersTableRepository.findOne(userId);
		if (user != null) {
			if (user.getUserPass().equals(userPass)) {

				// login_users_logにデータが無いこと。あればそのままs_passを出力
				List<LoginUsersLogTable> loginLogList = loginUsersLogTableRepository.findByUserIdAndImeiAndLogin(userId,
						imei, true);

				// すでにログイン中
				if (!loginLogList.isEmpty()) {
					resultMap.put("sucsess", "1");
					resultMap.put("s_pass", loginLogList.get(0).getsPass());
					return resultMap;
				}

				// ログイン情報の登録準備
				LoginUsersLogTable insData = new LoginUsersLogTable();
				insData.setUserId(userId);
				// imei登録
				insData.setImei(imei);
				// s_pass生成
				String sPassData;
				sPassData = UUID.randomUUID().toString();
				sPassData = userId + sPassData;
				insData.setsPass(sPassData);
				insData.setLogin(true);

				loginUsersLogTableRepository.save(insData);

				// return JSON
				resultMap.put("sucsess", "1");
				resultMap.put("s_pass", sPassData);
				return resultMap;
			}
		}

		return resultMap;
	}

	/**
	 * ユーザーアカウント作成
	 * in: /userRegist?user_id=***&user_pass=***&user_mail=***
	 * out: JSON型
	 * created: 1/0
	 * user_id: ""/"エラー内容（使用済みなど）"
	 * user_pass: ""/"エラー内容"
	 * user_mail: ""/"エラー内容"
	 * @param userId
	 * @param userPass
	 * @param userMail
	 * @return JSON
	 *
	 */
	@RequestMapping(value = "/userRegist", method = RequestMethod.GET)
	public Map<String, Object> userRegist(
			@RequestParam(name = "user_id", defaultValue = "") String userId,
			@RequestParam(name = "user_pass", defaultValue = "") String userPass,
			@RequestParam(name = "user_mail", defaultValue = "") String userMail) {

		String created = "1";
		String userIdComment = "";
		String userPassComment = "";
		String userMailComment = "";

		if (userId.length() == 0) {
			created = "0";
			userIdComment = "user_id情報無し;";
		}
		if (userPass.length() == 0) {
			created = "0";
			userPassComment = "user_pass情報無し;";
		}
		if (userMail.length() == 0) {
			created = "0";
			userMailComment = "user_mail情報無し;";
		} else {
			if (!this.mailCheck(userMail)) {
				created = "0";
				userMailComment = "user_mail不適正";
			}
		}

		UsersTable userData = usersTableRepository.findOne(userId);
		if (userData != null) {
			created = "0";
			userIdComment = userIdComment + "登録済みのuser_idです。;";
		}

		if (created.equals("1")) {
			userData = new UsersTable();
			userData.setUserId(userId);
			userData.setUserPass(userPass);
			userData.setUserMail(userMail);
			usersTableRepository.save(userData);
		}

		Map<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("created", created);
		resultMap.put("user_id", userIdComment);
		resultMap.put("user_pass", userPassComment);
		resultMap.put("user_mail", userMailComment);
		return resultMap;
	}

	/**
	 * ユーザーの情報確認
	 * 今はメールのみ by.2018.03.15
	 * err: ""/"コメント"
	 * user_mail: "メールアドレス"/""
	 * @param userId
	 * @param userPass
	 * @return JSON
	 */
	@RequestMapping(value = "/userData", method = RequestMethod.GET)
	public Map<String, Object> userData(
			@RequestParam(name = "user_id", defaultValue = "") String userId,
			@RequestParam(name = "user_pass", defaultValue = "") String userPass) {

		Map<String, Object> resultMap = new HashMap<String, Object>();

		String sucsess = "1";
		String userIdComment = "";
		String userPassComment = "";
		String user_mail = "";

		UsersTable userData = usersTableRepository.findOne(userId);

		if (userData == null) {
			sucsess = "0";
			userIdComment = "対象ユーザーがいません。";
		} else if (!userData.getUserPass().equals(userPass)) {
			sucsess = "0";
			userPassComment = "パスワードが違います。";
		} else {
		// 問題ないとき
			user_mail = userData.getUserMail();
		}

		resultMap.put("sucsess", sucsess);
		resultMap.put("user_id", userIdComment);
		resultMap.put("user_pass", userPassComment);
		resultMap.put("user_mail", user_mail);

		return resultMap;
	}

	/**
	 * ２－４．ユーザーパスワードorメール変更） ※s_pass必須
	 * in: /userChangeData?user_id=***&s_pass=***&user_old_pass=****&user_new_pass=****&user_mail=****
	 * out: JSON
	 *  sucsess: "1"/"0"
	 *  s_pass: ""/"エラー内容"
	 *  user_pass: ""/"エラー内容"
	 *  user_mail: ""/"エラー内容"
	 * @param userId
	 * @param sPass
	 * @param userOldPass
	 * @param userNewPass
	 * @param userMail
	 * @return
	 */
	@RequestMapping(value = "/userChangeData", method = RequestMethod.GET)
	public Map<String, Object> userChangeData(
			@RequestParam(name = "user_id", defaultValue = "") String userId,
			@RequestParam(name = "s_pass", defaultValue = "") String sPass,
			@RequestParam(name = "user_old_pass", defaultValue = "") String userOldPass,
			@RequestParam(name = "user_new_pass", defaultValue = "") String userNewPass,
			@RequestParam(name = "user_mail", defaultValue = "") String userMail) {

		Map<String, Object> resultMap = new HashMap<String, Object>();
		String sucsess = "1";
		String sPassComment = "";
		String userPassComment = "";
		String userMailComment = "";

		List<LoginUsersLogTable> userLoginData = loginUsersLogTableRepository.findByUserIdAndSPassAndLogin(userId,
				sPass, true);
		UsersTable userData = usersTableRepository.findOne(userId);

		// 存在の確認
		if (userData == null) {
			sucsess = "0";
		}
		// 許可の確認
		if (userLoginData.isEmpty()) {
			sucsess = "0";
			sPassComment = "ログインしてません。";
		}

		if (sucsess.equals("1")) {
			if (userOldPass.length() != 0 && userNewPass.length() != 0) {
				if (userData.getUserPass().equals(userOldPass)) {
					userData.setUserPass(userNewPass);
				} else {
					sucsess = "0";
					userPassComment = "パスワードが違います。";
				}
			}

			if (userMail.length() != 0) {
				if (this.mailCheck(userMail)) {
					userData.setUserMail(userMail);
				} else {
					sucsess = "0";
					userMailComment = "メールアドレスが不適正です。";
				}
			}
		}

		if (sucsess.equals("1")) {
			usersTableRepository.save(userData);
		}

		resultMap.put("sucsess", sucsess);
		resultMap.put("s_pass", sPassComment);
		resultMap.put("user_pass", userPassComment);
		resultMap.put("user_mail", userMailComment);

		return resultMap;
	}

	/**
	 * ログアウトする。
	 * @param userId
	 * @param imei
	 * @param s_pass
	 * @return ""
	 */
	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public String logout(
			@RequestParam(name = "user_id", defaultValue = "") String userId,
			@RequestParam(name = "imei", defaultValue = "") String imei,
			@RequestParam(name = "s_pass", defaultValue = "") String sPass) {

		List<LoginUsersLogTable> userDatas;

		userDatas = loginUsersLogTableRepository.findByLoginAndUserId(true, userId);
		for (LoginUsersLogTable userData : userDatas) {
			userData.setLogin(false);
			loginUsersLogTableRepository.save(userData);
		}

		userDatas = loginUsersLogTableRepository.findByLoginAndImei(true, imei);
		for (LoginUsersLogTable userData : userDatas) {
			userData.setLogin(false);
			loginUsersLogTableRepository.save(userData);
		}

		userDatas = loginUsersLogTableRepository.findByLoginAndSPass(true, sPass);
		for (LoginUsersLogTable userData : userDatas) {
			userData.setLogin(false);
			loginUsersLogTableRepository.save(userData);
		}

		return "";
	}

	/**
	 * 音声検索（検索～あいまい検索） ※s_pass必須
	 * in: /searchMusic?s_pass=*** // 全件
	 * in: /searchMusic?s_pass=***&namelike=**** // nameのところだけであいまい検索
	 * in: /searchMusic?s_pass=***&like=**** // name, commentのところであいまい検索
	 * out: JSON型。そのまま出力。
	 *  [[{"music_id": "***"}, {"music_name": "****"}, {"music_url": "***"}, {"music_comment": "*****"}],[*****],...]
	 *
	 * @param sPass
	 * @param namelike
	 * @param like
	 * @return
	 */
	@RequestMapping(value = "/searchMusic", method = RequestMethod.GET)
	public List<MusicUrlTable> searchMusic(
			@RequestParam(name = "s_pass", defaultValue = "") String sPass,
			@RequestParam(name = "namelike", defaultValue = "") String namelike,
			@RequestParam(name = "like", defaultValue = "") String like) {

		List<MusicUrlTable> musicList = new ArrayList<MusicUrlTable>();

		if (!this.sPassCheck(sPass)) {
			return musicList;
		} else {

			if (like.length() != 0) {
				// name, commentから検索
				like = "%" + like + "%";
				musicList = musicUrlTableRepository.findByMusicNameLikeOrMusicCommentLike(like, like);
			} else if (namelike.length() != 0) {
				// nameから検索
				namelike = "%" + namelike + "%";
				musicList = musicUrlTableRepository.findByMusicNameLike(namelike);
			} else {
				// 全件
				musicList = musicUrlTableRepository.findAll();
			}
		}
		return musicList;
	}

	/**
	 * 音声追加 コメントは無しでもOK ※s_pass必須
	 * in: /addMusic?s_pass=***&music_name=***&music_url****
	 * in: /addMusic?s_pass=***&music_name=***&music_url****&music_comment=***
	 * out: JSON型
	 *  sucsess: "1"/"0"
	 *  s_pass: ""/"エラー内容"
	 *  music_name: ""/"エラー内容"
	 *  music_url: ""/"エラー内容"
	 *  music_comment: ""/"エラー内容"
	 * @param sPass
	 * @param musicName
	 * @param musicUrl
	 * @param musicComment
	 * @return
	 */
	@RequestMapping(value = "/addMusic", method = RequestMethod.GET)
	public Map<String, Object> addMusic(
			@RequestParam(name = "s_pass", defaultValue = "") String sPass,
			@RequestParam(name = "music_name", defaultValue = "") String musicName,
			@RequestParam(name = "music_url", defaultValue = "") String musicUrl,
			@RequestParam(name = "music_comment", defaultValue = "") String musicComment) {

		Map<String, Object> resultMap = new HashMap<String, Object>();

		String sucsess = "1";
		String sPassComment = "";
		String musicNameComment = "";
		String musicUrlComment = "";
		String musicCommentComment = "";

		if (!this.sPassCheck(sPass)) {
			sucsess = "0";
			sPassComment = "不適正な接続";
		} else {

			if (musicName.length() == 0) {
				sucsess = "0";
				musicNameComment = "music_nameがありません。";
			}

			boolean connectionFlag = false;
			if (musicUrl.length() == 0) {
				sucsess = "0";
				musicUrlComment = "music_urlがありません。";
			} else {
				try {
					HttpURLConnection con = (HttpURLConnection) (new URL(musicUrl)).openConnection();
					con.setRequestMethod("HEAD");
					con.connect();
					int response = con.getResponseCode();
					con.disconnect();

					if (response == HttpURLConnection.HTTP_OK) {
						connectionFlag = true;
					}

				} catch (MalformedURLException e) {
					//				e.printStackTrace();
				} catch (IOException e) {
					//				e.printStackTrace();
				} finally {
					if (!connectionFlag) {
						sucsess = "0";
						musicUrlComment = "接続先がありません。";
					}
				}
			}

			if (sucsess.equals("1")) {

				MusicUrlTable musicData = new MusicUrlTable();
				musicData.setMusicName(musicName);
				musicData.setMusicUrl(musicUrl);
				musicData.setMusicComment(musicComment);

				musicUrlTableRepository.save(musicData);
			}
		}

		resultMap.put("sucsess", sucsess);
		resultMap.put("s_pass", sPassComment);
		resultMap.put("music_name", musicNameComment);
		resultMap.put("music_url", musicUrlComment);
		resultMap.put("music_comment", musicCommentComment);
		return resultMap;
	}

	/**
	 * POSTコメントの入手 ※s_pass必須
	 * in: /getPostComment?s_pass=*****&music_id=***
	 * out: JSON型
	 *  sucsess: "1"/"0"
	 *  s_pass: ""/"エラー内容"
	 *  music_id: ""/"エラー内容"
	 *  post_comment_list: [{post_comment: "コメント内容"},{post_comment: "コメント内容"},{****}.....]
	 * @param sPass
	 * @param musicId
	 * @return
	 */
	@RequestMapping(value = "/getPostComment", method = RequestMethod.GET)
	public Map<String, Object> getPostComment(
			@RequestParam(name = "s_pass", defaultValue = "") String sPass,
			@RequestParam(name = "music_id", defaultValue = "") String musicId) {

		Map<String, Object> resultMap = new HashMap<String, Object>();

		String sucsess = "1";
		String sPassComment = "";
		String musicIdComment = "";
		List<MusicPostCommentTable> postCommentList = new ArrayList<MusicPostCommentTable>();

		if (!this.sPassCheck(sPass)) {
			sucsess = "0";
			sPassComment = "不適正な接続";
		} else {

			if (musicId.length() == 0) {
				sucsess = "0";
				musicIdComment = "music_idを入力してください。";
			} else {
				try {
					int musicIdInt = Integer.parseInt(musicId);

					if (musicUrlTableRepository.findOne(musicIdInt) == null) {
						sucsess = "0";
						musicIdComment = "music_idが存在しません。";
					} else {
						postCommentList = musicPostCommentTableRepository.findByMusicId(musicIdInt);
					}
				} catch (NumberFormatException e) {
//					e.printStackTrace();
					sucsess = "0";
					musicIdComment = "music_idが不適正です。";
				}
			}
		}

		resultMap.put("sucsess", sucsess);
		resultMap.put("s_pass", sPassComment);
		resultMap.put("music_id", musicIdComment);
		resultMap.put("post_comment_list", postCommentList);
		return resultMap;
	}





	/**
	 * POSTコメントの投稿 ※s_pass必須
	 * in: /addPostComment?s_pass=*****&music_id=***&post_comment=***
	 * out: JSON型
	 *  sucsess: "1"/"0"
	 *  s_pass: ""/"エラー内容"
	 *  music_id: ""/"エラー内容"
	 *  post_comment: ""/"エラー内容"
	 * @param sPass
	 * @param musicId
	 * @param postComment
	 * @return
	 */
	@RequestMapping(value="/addPostComment", method=RequestMethod.GET)
	public Map<String, Object> addPostComment(
			@RequestParam(name = "s_pass", defaultValue = "") String sPass,
			@RequestParam(name = "music_id", defaultValue = "") String musicId,
			@RequestParam(name = "post_comment", defaultValue = "") String postComment) {

		Map<String, Object> resultMap = new HashMap<String, Object>();

		String sucsess = "1";
		String sPassComment = "";
		String musicIdComment = "";
		String postCommentComment = "";


		if (!this.sPassCheck(sPass)) {
			sucsess = "0";
			sPassComment = "不適正な接続";
		} else {
			if (musicId.length() == 0) {
				sucsess = "0";
				musicIdComment = "music_idを入力してください。";
			} else if (postComment.length() == 0) {
				sucsess = "0";
				postCommentComment = "post_commentを入力してください。";
			} else {
				try {
					int musicIdInt = Integer.parseInt(musicId);


					if (musicUrlTableRepository.findOne(musicIdInt) == null) {
						sucsess = "0";
						musicIdComment = "music_idが存在しません。";
					} else {

						MusicPostCommentTable addPostCommentData = new MusicPostCommentTable();
						addPostCommentData.setMusicId(musicIdInt);

						List<LoginUsersLogTable> users = loginUsersLogTableRepository.findByLoginAndSPass(true, sPass);
						addPostCommentData.setUserId(users.get(0).getUserId());

						addPostCommentData.setPostComment(postComment);

						musicPostCommentTableRepository.save(addPostCommentData);
					}
				} catch (NumberFormatException e) {
//					e.printStackTrace();
					sucsess = "0";
					musicIdComment = "music_idが不適正です。";
				}
			}
		}


		resultMap.put("sucsess", sucsess);
		resultMap.put("s_pass", sPassComment);
		resultMap.put("music_id", musicIdComment);
		resultMap.put("post_comment", postCommentComment);
		return resultMap;
	}









	@RequestMapping(value = "/test1")
	public Map<String, Object> test1() {

		Map<String, Object> map = new HashMap<String, Object>();
		Map<String, Object> map2 = new HashMap<String, Object>();
		List<Object> list = new ArrayList<Object>();

		map.put("test", "testData");
		map2.put("map2data", "map2desu");
		map2.put("aaa", "");
		list.add("aaaa");
		list.add("bbb");
		map2.put("list", list);
		map.put("map", map2);
		map.put("boolean=true", true);

		return map;
	}

	/**
	 * メールチェック
	 * OKならtrue
	 * @param mail
	 * @return true/false
	 */
	private boolean mailCheck(String mail) {
		String ptnStr = "[\\w\\.\\-]+@(?:[\\w\\-]+\\.)+[\\w\\-]+";
		Pattern ptn = Pattern.compile(ptnStr);
		Matcher mc = ptn.matcher(mail);
		if (mc.matches()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * SPassのチェック。
	 * @param sPass
	 * @return true/false
	 */
	private boolean sPassCheck(String sPass) {
		if (sPass.length() == 0) {
			return false;
		}

		List<LoginUsersLogTable> users = loginUsersLogTableRepository.findByLoginAndSPass(true, sPass);

		if (users.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}

}
