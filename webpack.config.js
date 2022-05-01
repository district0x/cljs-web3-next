module.exports = {
  mode: 'production',
  module: {
    rules: [
      {
        test: /\.css$/i,
        use: ["style-loader", "css-loader"],
      },
      {
        test: /\.(ttf|eot|svg)$/,
        use: {
          loader: 'file-loader',
          options: {
            name: 'fonts/[hash].[ext]',
          },
        },
      },
      {
        test: /\.(woff|woff2)$/,
        use: {
          loader: 'url-loader',
          options: {
            name: 'fonts/[hash].[ext]',
            limit: 5000,
            mimetype: 'application/font-woff',
          },
        },
      }
    ],
  },
  node: {
    fs: 'empty'
  },
  entry: './src/cljs_web3_next/js/index.js',
  output: {
    filename: 'index_bundle.js'
  }
};
