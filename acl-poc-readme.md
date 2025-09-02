### Business need
- Accelerate business-driven permission changes. Provide proactive coverage for future content and fast, low‑risk reactive updates when policies evolve.

### Recommended approach: AC Tool + proactive ACLs
- Declarative YAML for groups/ACLs; idempotent apply on deploy/startup.
- Proactive ACEs on stable parents with rep:glob (e.g., members-only/**) so future pages/assets inherit instantly—no redeploy.
- Governance via role groups (IMS/IdP) so most changes are “membership-only,” not new code.
- Reference: Netcentric AC Tool [README](https://github.com/Netcentric/accesscontroltool/blob/develop/README.md).

### What this POC implements
- Groups:
  - `wknd-standard-authors`: baseline, blocked in members-only.
  - `wknd-authors-members`: allowed in members-only.
- Pages:
  - `/content/wknd/language-masters/en/magazine/members-only`: deny standard authors; allow members-authors (modify/add/remove and jcr:content props).
- DAM parity (proactive):
  - `/content/dam/wknd-shared/en/magazine` with rep:glob `members-only` and `members-only/**`:
    - deny writes for standard authors; allow writes for members-authors.
  - Works whether or not the folder exists yet.
- Tests (author-only):
  - `ACLMembersOnlyIT` verifies:
    - Page property edits: blocked for standard authors inside members-only; allowed for members-authors.
    - DAM upload: denied for standard authors; allowed for members-authors when folder exists (ignores if absent).
  - Test maintenance: when introducing new groups or protected paths, add/extend author-only Integration Tests to cover the expected allow/deny (page modify and, if applicable, DAM upload). Keep tests idempotent and self-cleaning.

### Why this beats ACS Packages and Repo Init
- Target-state enforcement with diffs and stale ACE cleanup; correct ACE ordering.
- Proactive inheritance (ancestor + rep:glob) avoids package coupling to existing nodes.
- YAML scales (variables/loops/runmodes), easy to audit; startup hook is cloud-friendly.

### Reactive changes are faster, too
- Many requests are group membership only (IMS/IdP)—instant, no code.
- Small policy tweaks = one-line YAML change; AC Tool applies idempotently with clear logs and our Integration Tests for confidence.
- Hotfix-friendly: ACL YAML can be promoted independently of feature work.

### Release hygiene
- Use a branching strategy (e.g., GitFlow):
  - Keep ACL YAML in a dedicated permissions module/package.
  - For urgent ACLs, branch from main/release, run `it.tests`, promote independently.

### ACL change playbook
- Decide: membership vs YAML
  - If change is “who can edit/read” within established scopes → update IMS/IdP group membership:
    - Grant: add users to `wknd-authors-members`
    - Restrict: ensure users are not in that group (or stay in `wknd-standard-authors`)
    - Where: Adobe IMS Admin Console; or your enterprise IdP (Okta/Azure AD/ADFS) if syncing groups to IMS.
  - If change is “what/where” (new paths/scopes) → YAML update in AC Tool:
    - Prefer ancestor ACE with `rep:glob` to minimize future edits.
    - Keep changes small and isolated in the permissions module.

- Execute (membership-only)
  - Update group membership in IMS/IdP.
  - User re-login/token refresh; confirm with a quick author-side check (page edit or asset upload).

- Execute (YAML)
  - Edit YAML (add/remove ACE or tweak `rep:glob`).
  - Commit on a short-lived branch; add/extend `it.tests` for any new groups/paths and run them locally (author-only).
  - Deploy; verify AC Tool Execution Logs and re-run targeted tests.

- Verify
  - Author-side “can/can’t” checks:
    - Pages: property set on inside/outside paths.
    - DAM: small asset upload under the scoped folder.
  - Check AC Tool logs: confirm “SUCCESS (X authorizables/Y ACLs changed)” or no-op.

- Rollback
  - Membership: remove user from the group.
  - YAML: revert commit; redeploy; AC Tool restores previous target state automatically.

- Guardrails
  - Don’t ship rep:policy nodes in packages for business content.
  - Keep ACL logic in AC Tool; keep membership in IMS/IdP.
  - Avoid broad denies; favor precise allows plus minimal, well-scoped denies.
  - Keep ACE counts lean (performance) and use restrictions instead of duplicating entries.
  - Maintain tests alongside ACL changes so new groups/permissions are always validated.


### Branching strategy (stay nimble)
- To help ensure ACL development requests can be deployed to production quickly, ensure that a branching strategy is in place for all development (e.g., GitFlow or similar). This is a general best practice which facilitates hotfix or maintenance releases for urgent changes, while keeping these changes separate from features in longer running development cycles. 
- When deployed, the AC Tool install/startup hook applies changes immediately.
- Reference: GitFlow overview by Atlassian: [https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)

#### External repositories & PR validation (Cloud Manager)
- Cloud Manager can integrate with external repos (GitHub Enterprise, GitLab, Bitbucket) and validate pull requests via webhooks; Azure DevOps support is available via a private beta (Bring Your Own Git). See: [External repositories & PR validation](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/implementing/using-cloud-manager/managing-code/external-repositories#validation-of-pull-requests-with-webhooks).
- This integration allows webhooks from your repository to trigger events in Cloud Manager. For example, Pull Requests can run automatic code quality pipelines, and merges to a branch can deploy to a Non‑Prod environment for testing. 
- This automation may facilitate faster deployments for maintenance releases.  
- If applicable, request access to the beta as described in the article (Bring Your Own Git) and configure the required webhooks so PRs and pushes initiate validation in Cloud Manager.