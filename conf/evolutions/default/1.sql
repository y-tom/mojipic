# picture_properties schema

# --- !Ups

create table picture_properties(
                                   picture_id bigint(20) not null auto_increment, -- 画像ID(PK)
                                   status varchar(255) not null,                  -- 画像の処理状態
                                   twitter_id bigint(20) not null,                -- 投稿ユーザーのTwitter ID
                                   file_name varchar(255) not null,               -- アップロードされたファイル名
                                   content_type varchar(255) not null,            -- MIMEタイプ(image/png など)
                                   overlay_text varchar(255) not null,            -- 動画に重ねるコメント文字列
                                   overlay_text_size int(4) not null,             -- コメント文字サイズ
                                   original_filepath varchar(255) not null,       -- 元画像の保存先
                                   converted_filepath varchar(255),              -- 変換後画像の保存先
                                   created_time datetime not null,               -- 作成日時

                                   primary key (picture_id),

                                   index(twitter_id),    -- ユーザー検索用
                                   index(created_time)   -- 作成日時検索用
) engine=innodb charset=utf8mb4;

# --- !Downs

-- テーブル削除(ロールバック時)
drop table picture_properties;