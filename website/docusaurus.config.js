// @ts-check
// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

const projectTitle = 'Sealed Monad';
const organizationName = 'theiterators';
const editDocsUrl = 'https://github.com/theiterators/sealed-monad/tree/master/docs/';
const docsPath = '../docs';
const navbarTitle = 'Sealed Monad';
const projectGitHubUrl = 'https://github.com/theiterators/sealed-monad';

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'Sealed Monad Documentation',
  tagline: 'Scala library for elegant, business logic-oriented error handling with clean, linear code flow',
  url: 'https://theiterators.github.io',
  baseUrl: '/sealed-monad/',
  trailingSlash: false,
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: organizationName, // Usually your GitHub org/acuser name.
  projectName: 'sealed-monad', // Usually your repo name.

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          routeBasePath: '/', // This makes docs the root
          sidebarPath: require.resolve('./sidebars.js'),
          path: docsPath,
          // Please change this to your repo.
          editUrl: editDocsUrl,
          showLastUpdateAuthor: false,
          showLastUpdateTime: false,
        },
        // Disable the default landing page
        blog: false,
        pages: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
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
            to: '/',
            position: 'left',
            label: 'Documentation',
          },
          {
            href: projectGitHubUrl,
            label: 'GitHub',
            position: 'right',
          },
        ],
      },
      sidebar: {
        hideable: true,
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
