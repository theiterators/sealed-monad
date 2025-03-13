# Sealed Monad Documentation Website

This website is built using [Docusaurus 2](https://docusaurus.io/), a modern static website generator.

## Documentation Structure

The documentation is organized as follows:

1. **Introduction** - Overview of Sealed Monad
2. **Motivations & Core Concepts** - Why Sealed Monad was created and the main ideas
3. **Practical Use Cases** - Examples of using Sealed Monad in different scenarios
4. **API Reference** - Comprehensive documentation of the Sealed Monad API
5. **Best Practices** - Guidelines for using Sealed Monad effectively
6. **Comparison with Other Approaches** - How Sealed Monad differs from other error handling approaches
7. **Migration Guide** - How to migrate existing code to use Sealed Monad

## Installation

```
yarn
```

## Local Development

```
yarn start
```

This command starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

## Build

```
yarn build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

## Deployment

The website is automatically deployed to GitHub Pages through GitHub Actions. Any changes to the documentation will be reflected on the public documentation site after they're merged to the main branch.

For manual deployment:

Using SSH:
```
USE_SSH=true yarn deploy
```

Not using SSH:
```
GIT_USER=<Your GitHub username> yarn deploy
```

## Contributing to Documentation

If you find any issues or want to improve the documentation:

1. Clone the repository
2. Make changes to the relevant documentation files in the `/docs` directory
3. Build and test locally
4. Create a pull request with your changes

We welcome all contributions to make the documentation more helpful and comprehensive!