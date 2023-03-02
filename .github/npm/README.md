kolmafia
=======

Install this dependency to your mafia JS script to get TypeScript typings for the KoLmafia standard library.

If you are using a bundler (which you probably are) you will need to mark this as an external module so that the `require("kolmafia")` is kept in tact.

For example, in `webpack` you would add the following to your config

```
  externals: {
    kolmafia: 'commonjs kolmafia',
  }
```