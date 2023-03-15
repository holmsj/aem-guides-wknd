'use strict';

const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TSConfigPathsPlugin = require('tsconfig-paths-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const ESLintPlugin = require('eslint-webpack-plugin');


const SOURCE_ROOT = __dirname + '/src/main';
const RESOURCE_PATH = '/assets'
const SCRIPT_ENTRY = "/main.ts";
const BRANDS_PATH = "/webpack"
const SLASH = "/";
// const CLIENTLIB_FOLDERS = ['base','components', 'dependencies', 'site']
const CLIENTLIB_FOLDERS = ['base']

const resolve = {
    extensions: ['.js', '.ts'],
    plugins: [new TSConfigPathsPlugin({
        configFile: './tsconfig.json'
    })]
};
// ui.frontend/src/main/webpack/base/main.ts
module.exports = {
    resolve: resolve,
    entry: CLIENTLIB_FOLDERS.reduce((entryPaths, clientLibFolder) => {
        entryPaths[clientLibFolder] = SOURCE_ROOT + BRANDS_PATH + SLASH + clientLibFolder + SCRIPT_ENTRY;
        return entryPaths;
    }, {}),
    optimization: {
        emitOnErrors: false,
    },
    output: {
        filename: (chunkData) => {
            return chunkData.chunk.name === 'dependencies' ? 'clientlib-dependencies/[name].js' : 'clientlib-[name]/[name].js';
        },
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
                test: /\.(sa|sc|c)ss$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    "css-loader",
                    "postcss-loader",
                    "sass-loader",
                ],
            },
        ]
    },
    plugins: [
        new CleanWebpackPlugin(),
        new MiniCssExtractPlugin({
            filename: 'clientlib-[name]/[name].css'
        })
        // ,
        // new CopyWebpackPlugin({
        //     patterns: CLIENTLIB_FOLDERS.map(
        //         (brand) => (
        //             {
        //                 from: path.resolve(__dirname, SOURCE_ROOT + BRANDS_PATH + SLASH + brand + RESOURCE_PATH),
        //                 to: "./clientlib-" + brand + RESOURCE_PATH
        //             }
        //         )
        //     ),
        // }),
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
