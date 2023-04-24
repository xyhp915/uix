# CHANGELOG

## 0.9.1

### Improvements

- Fixed incorrect return value of `use-state` hook on JVM [99b85f](https://github.com/pitch-io/uix/commit/99b85f69f5f6402c9b2e0629e0b42913de692330)
- Fixed Hooks deps linter missing reporting deps which names are shadowing JS globals [2d11b8](https://github.com/pitch-io/uix/commit/2d11b818b8948935feb0689387bf082b64e49db8)
- Use `createRoot` in tests [32e0bc](https://github.com/pitch-io/uix/commit/32e0bc98fca5ba9433f714bada4f4191b98b5955)

## 0.9.0

### New

- Public API for linter plugins [#86](https://github.com/pitch-io/uix/pull/86)
- Deps linting enabled by default [25cc16](https://github.com/pitch-io/uix/commit/25cc16a5787d30492e17b7d3e254d351e45fc6e9)
- Added support for dynamic element types [14d5b7](https://github.com/pitch-io/uix/commit/14d5b7d29897c314a7d4a76570f47c58326a5081)
- Added clj-kondo config [#97](https://github.com/pitch-io/uix/pull/97)
- Added `error-boundary` component [#98](https://github.com/pitch-io/uix/pull/98)
- Added support for most React Hooks for SSR on JVM [#104](https://github.com/pitch-io/uix/pull/104)

### Improvements

- Improved missing deps reporting [5567dc](https://github.com/pitch-io/uix/commit/5567dce02e00cfdeb1e2da4fcd18af2fb14c16f0)
- Fixed re-frame/subscribe linter [#92](https://github.com/pitch-io/uix/pull/92)
- Fixed `dangerouslySetInnerHTML` throwing on JVM [#102](https://github.com/pitch-io/uix/pull/102)

### Docs

- Documented internals [docs/internals.md](https://github.com/pitch-io/uix/blob/master/docs/internals.md)

## 0.8.1

### Improvements

- Fixed how react-dom API is exposed (react-dom vs react-dom/client)

## 0.8.0

### New

- React v18.2.0 API compatibility [#59](https://github.com/pitch-io/uix/pull/59)
- New hooks wrappers: `use-insertion-effect`, `use-deferred-value`, `use-transition`, `start-transition`, `use-id` and `use-sync-external-store`
- New `uix.dom` public API: `create-root`, `hydrate-root`, `render-root` and `unmount-root`

## 0.7.1

### Improvements

- Fix rules of hooks linter [f7276d](https://github.com/pitch-io/uix/commit/f7276decf191e0b804f7f393add91ebd982dcade)

## 0.7.0

### New

- Added `uix.core/fn` macro [924e4b](https://github.com/pitch-io/uix/commit/924e4b37f841120312054dac283c2b59125e7cda)

## 0.6.2

### Improvements

- Improved linter rule for missing `:key` attribute [1ee076](https://github.com/pitch-io/uix/commit/1ee076bf1f877e82f105a65faaa0e6586e7c4dc1)
- Make missing `:key` rule configurable [6f4873](https://github.com/pitch-io/uix/commit/6f4873af5881cda2e3a44e0c4223c399efcf79e4)

## 0.6.1

### Improvements

- Recursive class names stringifier [ebc178](https://github.com/pitch-io/uix/commit/ebc1787dce975a7608aab5549006b7a71aa327ea)

## 0.6.0

### Improvements

- Better code location pointing in linting errors [7116d7](https://github.com/pitch-io/uix/commit/7116d76f18d6d7d216ec410894142bd041550de8)

### New

- Linter rule to report on missing `:key` attribute [fed7d8](https://github.com/pitch-io/uix/commit/fed7d8e0e2532dd6edf82dc60425a7a6e062813b)

## 0.5.0

### Improvements

- Make re-frame linter check configurable [8ef493](https://github.com/pitch-io/uix/commit/8ef4932f88071377e16dde755c18a8e60aaf05e7)
- Improve linter's error messages [03609b](https://github.com/pitch-io/uix/commit/03609bd305c6706f830a24517999f6e90527ce35)

## 0.4.0

### Improvements

- Added linter check for non-reactive usage of re-frame subscriptions in UIx components via re-frame API [071650](https://github.com/pitch-io/uix/commit/0716507b6bfdcb28091879ef14958aae4300c751)

### Docs

- Added a section on [“Syncing with ratoms and re-frame”](https://github.com/pitch-io/uix/blob/master/docs/interop-with-reagent.md#syncing-with-ratoms-and-re-frame)
- Added a section about [“Utilities”](https://github.com/pitch-io/uix/blob/master/docs/utilities.md)

## 0.3.0

### Improvements

- Fixed shadowing for non-ns component var generated in dev-only code [4458ee](https://github.com/pitch-io/uix/commit/4458ee7c31aa87e98961140ba0fa2807f57d2de9)

### New

- Basic SSR on JVM [4a10c9](https://github.com/pitch-io/uix/commit/4a10c9b9282fadb2c58029d0786ceba77f4487f4)

## 0.2.0

### Improvements

- Improved missing deps check to account for vars shadowing [#73](https://github.com/pitch-io/uix/pull/73)

### New

- Added `^:lint-deps` meta for deps vector in hooks to opt-in for missing deps check [1dbb7d9](https://github.com/pitch-io/uix/commit/1dbb7d93d17941e3066e5d5a3029d0642868c8c0)
- Documented hooks linting [baa7b9](https://github.com/pitch-io/uix/commit/baa7b90850378102d89c4fa15022569d769c1bef)
