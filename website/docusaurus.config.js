// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

const projectTitle = 'sealed-monad';
const organizationName = 'theiterators';
const editDocsUrl = 'https://github.com/theiterators/sealed-monad/tree/master/docs/';
const docsPath = '../docs';
const navbarTitle = 'sealed-monad';
const projectGitHubUrl = 'https://github.com/theiterators/sealed-monad';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: projectTitle,
  tagline: 'Scala library for business logic oriented, for-comprehension-style error handling',
  url: 'https://theiterators.github.io',
  baseUrl: '/sealed-monad/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: organizationName, // Usually your GitHub org/user name.
  projectName: projectTitle, // Usually your repo name.

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          path: docsPath,
          // Please change this to your repo.
          editUrl: editDocsUrl,
          routeBasePath: '/', // Set docs as the root
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
        blog: false, // Disable blog
      }),
    ],
  ],
  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      navbar: {
        title: navbarTitle,
        logo: {
          alt: 'Logo',
          src: 'img/favicon.ico',
        },
        items: [
          {
            href: projectGitHubUrl,
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              {
                label: 'Introduction',
                to: '/',
              },
            ],
          },
          {
            title: 'Community',
            items: [
              {
                label: 'Twitter',
                href: 'https://twitter.com/iteratorshq',
              },
              {
                label: 'GitHub',
                href: projectGitHubUrl,
              }
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} <a href="https://www.iteratorshq.com">Iterators</a> sp. z o.o.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
        additionalLanguages: ['java','scala']
      },
    }),
};

module.exports = config;
