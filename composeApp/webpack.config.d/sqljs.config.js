// Webpack configuration for SQLDelight WASM support
// This copies the sql-wasm.wasm file and configures webpack fallbacks
const CopyWebpackPlugin = require('copy-webpack-plugin');
const path = require('path');

// Add resolve fallbacks for Node.js modules that SQLDelight might reference
config.resolve = config.resolve || {};
config.resolve.fallback = config.resolve.fallback || {};
Object.assign(config.resolve.fallback, {
    fs: false,
    path: false,
    crypto: false,
});

// Copy sql-wasm.wasm file to output directory
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            {
                // The webpack config is generated in build/wasm/packages/composeApp/
                // We need to go up 2 levels to reach build/wasm/ where node_modules is
                from: path.resolve(__dirname, '../../node_modules/sql.js/dist/sql-wasm.wasm'),
                // Copy to root of webpack output directory
                to: 'sql-wasm.wasm'
            }
        ]
    })
);
