'use strict';

const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TSConfigPathsPlugin = require('tsconfig-paths-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const ESLintPlugin = require('eslint-webpack-plugin');


const SOURCE_ROOT = __dirname + '/src';
const RESOURCE_PATH = '/assets'
const SCRIPT_ENTRY = "/main.ts";
const LIBRARIES_PATH = "/libraries"
const SLASH = "/";
const CLIENTLIB_FOLDERS = ['components','site']

const resolve = {
    extensions: ['.js', '.ts'],
    plugins: [new TSConfigPathsPlugin({
        configFile: './tsconfig.json'
    })]
};
module.exports = {
    resolve: resolve,
    entry: CLIENTLIB_FOLDERS.reduce((entryPaths, clientLibFolder) => {
        entryPaths[clientLibFolder] = SOURCE_ROOT + LIBRARIES_PATH + SLASH + clientLibFolder + SCRIPT_ENTRY;
        return entryPaths;
    }, {}),
    externals: {
        jquery: "jQuery"
    },
    optimization: {
        splitChunks: {
                chunks: 'all'
            }
    },
    output: {
        filename: 'clientlib-[name]/[name].js',
        path: path.resolve(__dirname, 'dist')
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                exclude: /node_modules/,
                use: [
                    {
                        loader: 'ts-loader'
                    },
                    {
                        loader: 'webpack-import-glob-loader',
                        options: {
                            url: false
                        }
                    }
                ]
            },
            {
                test: /\.(woff(2)?|ttf|eot)$/,
                type: 'asset/resource',
                generator: {
                    filename: './resources/assets/fonts/[name][ext]',
                },
            },
            {
                test: /\.(svg)$/,
                type: 'asset/resource',
                generator: {
                    filename: './resources/assets/images/[name][ext]',
                },
            },
            {
                test: /\.(sa|sc|c)ss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    {
                        loader: 'css-loader',
                        options: {
                            url: false
                        }
                    },
                    {
                        loader: 'postcss-loader',
                        options: {
                            postcssOptions: {
                              plugins: [
                                [
                                  "autoprefixer",
                                  {
                                    // Options
                                  },
                                ],
                              ],
                            },
                        },
                    },
                    {
                        loader: 'sass-loader',
                    },
                ],
            },
        ]
    },
    plugins: [
        new CleanWebpackPlugin(),
        new ESLintPlugin({
            extensions: ['js', 'ts', 'tsx']
        }),
        new MiniCssExtractPlugin({
            filename: 'clientlib-[name]/[name].css'
        })
        ,
        new CopyWebpackPlugin({
            patterns: CLIENTLIB_FOLDERS.map(
                (clientLibFolder) => (
                    {
                        from: path.resolve(__dirname, SOURCE_ROOT + LIBRARIES_PATH + SLASH + clientLibFolder + RESOURCE_PATH),
                        to: "./clientlib-" + clientLibFolder + RESOURCE_PATH
                    }
                )
            ),
        }),
    ],
    stats: {
        assetsSort: 'chunks',
        builtAt: true,
        children: false,
        chunkGroups: true,
        chunkOrigins: true,
        colors: false,
        errors: true,
        errorDetails: true,
        env: true,
        modules: false,
        performance: true,
        providedExports: false,
        source: false,
        warnings: true
    }
};
