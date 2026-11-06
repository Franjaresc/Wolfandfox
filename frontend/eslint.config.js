import js from '@eslint/js';
import prettier from 'eslint-config-prettier';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'src/api/schema.gen.d.ts'] },
  js.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    extends: [...tseslint.configs.strictTypeChecked],
    languageOptions: {
      parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname },
    },
  },
  {
    files: ['src/**/*.{ts,tsx}'],
    extends: [jsxA11y.flatConfigs.recommended, reactHooks.configs.flat.recommended],
    languageOptions: { globals: globals.browser },
  },
  {
    // User-facing text lives in src/messages.ts, so it can be translated later.
    files: ['src/**/*.tsx'],
    ignores: ['**/*.test.tsx', 'src/test/**'],
    rules: {
      'no-restricted-syntax': [
        'error',
        {
          selector: 'JSXText[value=/[^\\s\\p{P}]/u]',
          message: 'Move user-facing text to src/messages.ts.',
        },
        {
          selector: 'JSXAttribute[name.name=/^(aria-label|alt|placeholder|title)$/] > Literal',
          message: 'Move user-facing text to src/messages.ts.',
        },
      ],
    },
  },
  {
    files: ['**/*.js', '**/*.mjs', 'vite.config.ts', 'scripts/**'],
    languageOptions: { globals: globals.node },
  },
  prettier,
);
