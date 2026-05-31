import $ from 'jquery';
import 'materialize-css';
import Dropzone from 'dropzone';
import React from 'react';
import ReactDOM from 'react-dom';

// Dropzoneのドラッグ＆ドロップエリアの設定
Dropzone.options.filedropzone = {
  // サーバ側で受け取るファイルのパラメータ名
  paramName: 'file', // The name that will be used to transfer the file
  // アップロードできる最大ファイルサイズ
  maxFilesize: 2, // MB
  // Dropzone内に表示するデフォルトメッセージ
  dictDefaultMessage: '',
  // サムネイルの表示サイズ
  thumbnailHeight: 200,
  thumbnailWidth: 200,
  // Dropzone初期化時の処理
  init: function () {
    // ファイルが追加されたタイミングで実行
    this.on('addedfile', function (file) {
      // 表示用inputの値を、送信用hidden inputにコピーする
      $('#overlaytext').val($('#overlaytext-shown').val());
      $('#overlaytextsize').val($('#overlaytextsize-shown').val());
    });
  },

  // ファイル受け入れ時の処理
  accept: function (file, done) {
    // ファイルを受け入れる
    done();

    // Dropzoneが自動生成する不要な表示要素を削除
    $('.dz-details').remove();
    $('.dz-progress').remove();
    $('.dz-error-message').remove();
    $('.dz-success-mark').remove();
    $('.dz-error-mark').remove();
  }
};

// Materializeの設定
$(function () {
  // サイドメニューを有効化
  $('.button-collapse').sideNav();
});