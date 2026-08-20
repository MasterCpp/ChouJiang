# 023: Mobile Registration Language Copy

## Status

done

## Goal

Keep the public registration and success screens consistently Chinese or English, and keep the language switch reachable in the WeChat mobile browser.

## Background

In English mode, score questions still show the bilingual placeholder "请选择 / Select". The submitted screen shows both a Chinese and an English confirmation paragraph. On mobile, the fixed language switch sits behind the WeChat in-app browser navigation area.

## Scope

- Give score-select placeholders explicit Chinese and English system copy.
- Render one localized registration-success message instead of two paragraphs.
- Raise the public-page language switch on narrow screens.
- Do not alter event, question, option, or registration values entered by users.

## Acceptance Criteria

- English score selects show only "Select" and Chinese score selects show only "请选择".
- Registration confirmation shows one Chinese paragraph in Chinese mode or one English paragraph in English mode.
- The public-page switch remains above the mobile browser controls.

## Implementation Notes

Use `data-locale-zh` and `data-locale-en` for built-in copy, then apply the offset only to `.public-page` on narrow screens.
