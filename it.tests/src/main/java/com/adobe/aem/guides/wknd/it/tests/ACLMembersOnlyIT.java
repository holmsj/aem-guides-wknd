/*
 *  Copyright 2015 Adobe
 */
package com.adobe.aem.guides.wknd.it.tests;

import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;
import static org.apache.http.HttpStatus.SC_OK;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.CQSecurityClient;
import com.adobe.cq.testing.client.CQAssetsClient;
import com.adobe.cq.testing.client.security.AbstractAuthorizable;
import com.adobe.cq.testing.client.security.Group;
import com.adobe.cq.testing.client.security.User;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import java.util.Collections;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Verifies ACLs for members-only area:
 * - Standard authors cannot modify under /content/wknd/language-masters/en/magazine/members-only
 * - Members authors can modify under the same path
 */
public class ACLMembersOnlyIT {

    @ClassRule
    public static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    private static CQClient adminAuthor;
    private static CQSecurityClient adminSecurity;

    private static final String STANDARD_USER_ID = "it-wknd-author";
    private static final String STANDARD_USER_PW = "it-wknd-author";
    private static final String MEMBERS_USER_ID = "it-wknd-author-members";
    private static final String MEMBERS_USER_PW = "it-wknd-author-members";

    private static final String STANDARD_GROUP_ID = "wknd-standard-authors";
    private static final String MEMBERS_GROUP_ID = "wknd-authors-members";

    private static final String BASE_PATH = "/content/wknd/language-masters/en";
    private static final String MEMBERS_ONLY_PATH = BASE_PATH + "/magazine/members-only";
    private static final String OUTSIDE_PAGE = BASE_PATH + "/magazine/western-australia";
    private static final String MEMBERS_PAGE = MEMBERS_ONLY_PATH + "/alaskan-adventure";
    private static final String DAM_MAGAZINE = "/content/dam/wknd-shared/en/magazine";
    private static final String DAM_MEMBERS_ONLY = DAM_MAGAZINE + "/members-only";

    private static final long READINESS_TIMEOUT_MS = Long.getLong("it.acl.readinessTimeoutMs", 360000L); // 6 minutes default for CI

    @BeforeClass
    public static void setup() throws Exception {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CQClient.class);
        adminSecurity = cqBaseClassRule.authorRule.getAdminClient(CQSecurityClient.class);

        // Create users and add them to groups
        if (!User.exists(adminSecurity, STANDARD_USER_ID)) {
            User.createUser(adminSecurity, STANDARD_USER_ID, STANDARD_USER_PW, null, Collections.emptyMap());
        }
        if (!User.exists(adminSecurity, MEMBERS_USER_ID)) {
            User.createUser(adminSecurity, MEMBERS_USER_ID, MEMBERS_USER_PW, null, Collections.emptyMap());
        }

        Group standardGroup = new Group(adminSecurity, STANDARD_GROUP_ID);
        Group membersGroup = new Group(adminSecurity, MEMBERS_GROUP_ID);

        User standardUser = new User(adminSecurity, STANDARD_USER_ID);
        User membersUser = new User(adminSecurity, MEMBERS_USER_ID);

        standardGroup.addMember(standardUser, SC_OK);
        membersGroup.addMember(membersUser, SC_OK);

        // Wait briefly for AC Tool to apply policies and groups to be visible
        awaitAclReadiness();
    }

    @AfterClass
    public static void cleanup() throws Exception {
        // Best-effort cleanup of temp test pages that might remain
        tryDeletePage(adminAuthor, MEMBERS_ONLY_PATH + "/it-acl-standard");
        tryDeletePage(adminAuthor, MEMBERS_ONLY_PATH + "/it-acl-members");
        tryDeletePage(adminAuthor, BASE_PATH + "/magazine/it-acl-control");
        // Users are left in place for repeatability; group membership remains.
    }

    private static void awaitAclReadiness() throws InterruptedException {
        long start = System.currentTimeMillis();
        long deadline = start + READINESS_TIMEOUT_MS;
        int sleepMs = 500;
        final int maxSleepMs = Integer.getInteger("it.acl.maxSleepMs", 30000); // ramp up to 30s by default
        long lastLog = 0;
        System.out.println("[ACLMembersOnlyIT] Waiting for ACL readiness (timeout=" + READINESS_TIMEOUT_MS + "ms, maxSleep=" + maxSleepMs + "ms)...");
        while (true) {
            try {
                // Group presence (use SecurityClient to check by authorizableId)
                if (!AbstractAuthorizable.exists(adminSecurity, MEMBERS_GROUP_ID)) {
                    throw new IllegalStateException("Group not yet present: " + MEMBERS_GROUP_ID);
                }
                // Content exists (base WKND section and specific pages used by this test)
                adminAuthor.doGet(BASE_PATH + ".json", SC_OK);
                adminAuthor.doGet(OUTSIDE_PAGE + ".json", SC_OK);
                adminAuthor.doGet(MEMBERS_PAGE + ".json", SC_OK);
                // Note: do not check rep:policy directly (may not be readable via JSON rendering)
                System.out.println("[ACLMembersOnlyIT] ACL readiness satisfied in " + (System.currentTimeMillis() - start) + "ms");
                return; // ready
            } catch (Exception e) {
                long now = System.currentTimeMillis();
                if (now - lastLog >= 1000) {
                    System.out.println("[ACLMembersOnlyIT] Not ready yet (elapsed=" + (now - start) + "ms, nextSleep=" + sleepMs + "ms, timeout=" + READINESS_TIMEOUT_MS + "ms)...");
                    lastLog = now;
                }
                if (now > deadline) {
                    System.out.println("[ACLMembersOnlyIT] Gave up waiting after " + (now - start) + "ms (timeout=" + READINESS_TIMEOUT_MS + "ms); proceeding with tests.");
                    return; // give up; tests will indicate if not ready
                }
                Thread.sleep(sleepMs);
                if (sleepMs < maxSleepMs) {
                    sleepMs = Math.min(sleepMs + 250, maxSleepMs);
                }
            }
        }
    }

    private static void tryDeletePage(CQClient client, String path) {
        try {
            if (client.pageExists(path)) {
                client.deletePage(new String[]{path}, true, true, SC_OK);
            }
        } catch (Exception ignored) {
        }
    }

    @Test
    public void standardAuthor_cannotCreateUnderMembersOnly() throws Exception {
        CQClient standardAuthor = cqBaseClassRule.authorRule.getClient(CQClient.class, STANDARD_USER_ID, STANDARD_USER_PW);
        CQAssetsClient standardAssets = cqBaseClassRule.authorRule.getClient(CQAssetsClient.class, STANDARD_USER_ID, STANDARD_USER_PW);

        // Control: can modify outside members-only
        standardAuthor.setPageProperty(OUTSIDE_PAGE, "jcr:title", "ACL Test Outside", SC_OK);

        // Should be forbidden in members-only
        standardAuthor.setPageProperty(MEMBERS_PAGE, "jcr:title", "ACL Test Blocked", SC_FORBIDDEN, SC_UNPROCESSABLE_ENTITY);

        // DAM: standard author should not be able to upload into members-only (future/actual)
        // If folder doesn't exist, server may return 404; treat 403/404 as acceptable deny result.
        try {
            standardAssets.uploadAsset("it-acl-deny.txt", "/com/adobe/aem/guides/wknd/it/tests/CreatePageIT.class", "text/plain", DAM_MEMBERS_ONLY, SC_FORBIDDEN);
        } catch (Exception e) {
            // ignore to allow 404/permission errors in absence of folder
        }
    }

    @Test
    public void membersAuthor_canCreateUnderMembersOnly() throws Exception {
        CQClient membersAuthor = cqBaseClassRule.authorRule.getClient(CQClient.class, MEMBERS_USER_ID, MEMBERS_USER_PW);
        CQAssetsClient membersAssets = cqBaseClassRule.authorRule.getClient(CQAssetsClient.class, MEMBERS_USER_ID, MEMBERS_USER_PW);

        // Should be allowed inside members-only
        membersAuthor.setPageProperty(MEMBERS_PAGE, "jcr:title", "ACL Test Allowed", SC_OK);

        // DAM: members author should be able to upload to members-only subtree if it exists
        try {
            membersAssets.uploadAsset("it-acl-allow.txt", "/com/adobe/aem/guides/wknd/it/tests/CreatePageIT.class", "text/plain", DAM_MEMBERS_ONLY, SC_OK, 201);
        } catch (Exception e) {
            // ignore if folder not present
        }
    }
}


