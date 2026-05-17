// Node.js標準のpathモジュールを読み込む
// 出力先パスを安全に組み立てるために使う
const path = require('path');
// webpack本体を読み込む
const webpack = require('webpack');

module.exports = {
  // 変換・結合の入口になるJSファイル
  // ここでimportしたライブラリもまとめて処理される
  entry: './app/views/index.js',

  // 圧縮などをしない設定
  // 学習・デバッグしやすくするため
  mode: 'none',

  output: {
    // 出力されるJSファイル名
    filename: 'main.js',
    // 出力先
    // public/javascripts/main.js に生成される
    path: path.resolve(__dirname, '../public/javascripts')
  },

  module: {
    rules: [
      {
        // .jsファイルを対象にする
        test: /\.js$/,
        use: [
          {
            // Babelを使ってJSを変換する
            loader: 'babel-loader',
            options: {
              presets: [
                // ES6以降のJSをブラウザ向けに変換する
                ['@babel/preset-env', { 'modules': false }],

                // ReactのJSXを変換する
                '@babel/preset-react'
              ]
            }
          }
        ],

        // 外部ライブラリは変換対象から外す
        exclude: /node_modules/,
      }
    ]
  },

  // 変換前のコードと対応づけるためのファイルを出す
  // ブラウザの開発者ツールでデバッグしやすくなる
  devtool: 'source-map',

  plugins: [
    // ブラウザにはNode.jsのprocessがないため、
    // 「processが存在しない」エラーを避ける
    new webpack.DefinePlugin({
      process: { env: {} }
    })
  ]
};