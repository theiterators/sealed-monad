/**
 * Creating a sidebar enables you to:
 - create an ordered group of docs
 - render a sidebar for each doc of that group
 - provide next/previous navigation

 The sidebars can be generated from the filesystem, or explicitly defined here.

 Create as many sidebars as you want.
 */

// @ts-check

/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  // Explicitly define the sidebar items in a logical order
  docs: [
    'intro',
    'motivations',
    'installation',
    'usecases',
    'api-reference',
    'best-practices',
    'comparison',
    'migration-guide',
    'faq',
    'conclusion'
  ]
};

module.exports = sidebars;
