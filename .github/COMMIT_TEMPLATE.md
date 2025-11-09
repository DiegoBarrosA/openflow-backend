# Commit Message Template

## Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

## Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

## Scope (optional)

- `auth`: Authentication related
- `api`: API endpoints
- `config`: Configuration
- `security`: Security related

## Subject

- Use imperative mood ("add" not "added")
- First letter lowercase
- No period at end
- Maximum 50 characters

## Body (optional)

- Explain what and why
- Reference issues if applicable

## Footer (optional)

- Reference issues: `Closes #123`
- Breaking changes: `BREAKING CHANGE: description`

## AI Assistance Tag

Add at the end of commit message:
```
[AI-Assisted: Composer]
```

## Examples

```
feat(auth): add JWT token refresh endpoint

Implements token refresh to extend session without re-login.
Token refresh endpoint accepts valid refresh token and returns
new access token.

[AI-Assisted: Composer]
```

```
fix(api): correct board deletion cascade

Fixes issue where deleting board did not properly cascade
delete related statuses and tasks.

Closes #45
[AI-Assisted: Composer]
```

