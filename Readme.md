# Call Recorder

A production-grade, completely offline Android Call Recorder built with Kotlin and Jetpack Compose.

---

# Vision

Build the best Android Call Recorder application with premium UI/UX, blazing-fast performance, zero internet dependency, and professional architecture.

This is **NOT** a demo project.

This is **NOT** a learning project.

This is intended to be a commercial-quality Android application with enterprise-level architecture and code quality.

Everything runs locally on the device.

No cloud.

No analytics.

No advertisements.

No user account.

No login.

No internet.

Everything belongs to the user.

---

# Core Philosophy

The application should feel like it was developed by a company such as:

- Google
- Samsung
- OnePlus
- Nothing
- Apple (design quality)

Every screen must feel polished.

Animations must be smooth.

Scrolling must be 60fps.

Code must be highly maintainable.

Architecture must be scalable.

Battery consumption must be minimal.

Memory usage must be optimized.

---

# Main Goals

The application should:

- Automatically detect phone calls
- Automatically record calls
- Save recordings locally
- Organize recordings
- Provide beautiful playback
- Search recordings instantly
- Consume almost no battery while idle
- Never require internet

---

# Offline First

This application is completely offline.

Never connect to any server.

Never make API requests.

Never upload recordings.

Never collect analytics.

Never request unnecessary permissions.

Never require internet permission.

Everything is stored locally.

---

# Technology Stack

## Language

Kotlin

---

## UI

Jetpack Compose

Material Design 3

Material You

---

## Architecture

Clean Architecture

MVVM

Repository Pattern

SOLID Principles

Feature Modular Architecture

---

## Dependency Injection

Hilt

---

## Database

Room (SQLite)

Flow

---

## Background Processing

Foreground Service

Broadcast Receiver

WorkManager

Coroutines

---

## Audio

MediaRecorder

MediaPlayer

AudioRecord (if required)

---

## Preferences

Jetpack DataStore

---

## Logging

Timber

---

## Navigation

Navigation Compose

---

## Async

Coroutines

Flow

---

## Image Loading

Coil

---

## Testing

JUnit

Mockk

Compose Testing

---

# Application Modules

```
app

core

common

data

database

domain

repository

di

receiver

service

worker

recorder

player

settings

storage

contacts

permissions

search

recordings

home

ui

navigation

utils
```

---

# Features

## Automatic Call Detection

Detect

- Incoming Call
- Outgoing Call
- Answered Call
- Missed Call
- Rejected Call
- Call End

---

## Automatic Recording

Automatically start recording.

Automatically stop recording.

Save recording.

Update database.

Return to idle.

---

## Recording Management

View all recordings

Rename

Delete

Favorite

Share

Move

Export

Restore

Search

Filter

Sort

Multi Select

Batch Delete

Batch Share

Batch Export

---

## Audio Player

Beautiful player

Seek bar

Playback speed

Repeat

Skip

Duration

Waveform

Share

Rename

Delete

Favorite

---

## Search

Search by

Phone number

Contact name

Date

Duration

Favorite

Notes

---

## Contacts

Automatically resolve contact names.

Display contact photos.

Unknown numbers supported.

---

## Dashboard

Today's recordings

Weekly recordings

Monthly recordings

Storage usage

Recent recordings

Favorites

Total recordings

Longest recording

Average duration

---

## Statistics

Daily

Weekly

Monthly

Total calls

Incoming

Outgoing

Average duration

Storage consumed

---

## Storage Manager

Storage usage

Largest recordings

Oldest recordings

Newest recordings

Remaining storage

Low storage warning

---

## Security

PIN Lock

Fingerprint

Biometric

App Lock

Hidden recordings

Secure playback

---

## Settings

Theme

Material You

Dark

Light

Audio Quality

Recording Folder

Ignored Contacts

Record Unknown Numbers

Record Everyone

Auto Delete

Notification Settings

App Lock

Backup Folder

Restore Folder

Language

---

# User Interface

Premium modern interface.

Material Design 3.

Dynamic colors.

Rounded corners.

Beautiful cards.

Smooth transitions.

Adaptive layouts.

Large typography.

Professional animations.

Modern bottom sheets.

Floating action buttons.

Responsive layouts.

---

# Battery Optimization

Application should remain idle most of the time.

No polling.

No infinite loops.

No unnecessary timers.

No background CPU usage.

Only react to Android system events.

Keep memory footprint low.

Release resources immediately after recording.

---

# Performance Goals

Cold start under one second.

Fast Room queries.

Lazy loading.

Minimal recomposition.

Efficient Compose state management.

Minimal allocations.

Optimized scrolling.

---

# Accessibility

Large fonts

TalkBack

High contrast

Proper content descriptions

Accessibility friendly

---

# Error Handling

Application should never crash.

Handle:

Permission denied

Recorder unavailable

Storage full

Microphone unavailable

Database failure

Unexpected exceptions

Graceful recovery

---

# Recording Format

Preferred:

AAC

Container:

M4A

Quality

Low

Medium

High

Filename

YYYY-MM-DD_HH-mm-ss_PHONE_NUMBER.m4a

---

# Database

Entity

Recording

Fields

- id
- phoneNumber
- contactName
- incoming
- duration
- date
- filePath
- size
- quality
- favorite
- deleted
- notes

---

# Notifications

Foreground notification

Minimal

Clean

Professional

Non-intrusive

---

# Folder Structure

```
recordings/

2026/

January/

February/

March/

...

December/
```

Automatically organize recordings by year and month.

---

# Code Standards

Use Kotlin coding conventions.

Follow SOLID.

Follow Clean Architecture.

Avoid duplicated code.

Create reusable components.

Document public APIs.

Avoid hardcoded values.

Use resource files.

Write production-quality code.

---

# UI Principles

Every screen should look premium.

No ugly dialogs.

Use modern bottom sheets.

Elegant typography.

Consistent spacing.

Beautiful loading states.

Beautiful empty states.

Professional error states.

Smooth transitions.

---

# Development Rules

Never generate unnecessary code.

Never create giant classes.

Keep files small.

Prefer composition over inheritance.

Prefer immutable state.

Use StateFlow.

Use Flow.

Avoid LiveData unless required.

Avoid deprecated APIs.

Prefer official Android Jetpack libraries.

---

# AI Agent Instructions

You are the lead Android architect for this project.

Always prioritize:

1. Stability
2. Performance
3. Battery efficiency
4. Clean Architecture
5. Beautiful UI
6. Scalability
7. Readability
8. Offline-first design

Never sacrifice architecture for convenience.

When implementing a feature:

1. Explain the architecture.
2. Generate production-ready code.
3. Explain important decisions.
4. Wait for confirmation before moving to the next feature.

Never generate the entire application at once.

Build incrementally.

Each feature must be complete before continuing.

Always assume this project will be maintained for many years.

Every line of code should be written with production quality in mind.
