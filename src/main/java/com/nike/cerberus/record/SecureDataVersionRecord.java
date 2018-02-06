/*
 * Copyright (c) 2018 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nike.cerberus.record;

import java.time.OffsetDateTime;

public class SecureDataVersionRecord {

    private String id;
    private String sdboxId;
    private String path;
    private String encryptedBlob;
    private String action;
    private String versionCreatedBy;
    private OffsetDateTime versionCreatedTs;
    private String actionPrincipal;
    private OffsetDateTime actionTs;

    public String getId() {
        return id;
    }

    public SecureDataVersionRecord setId(String id) {
        this.id = id;
        return this;
    }

    public String getSdboxId() {
        return sdboxId;
    }

    public SecureDataVersionRecord setSdboxId(String sdboxId) {
        this.sdboxId = sdboxId;
        return this;
    }

    public String getPath() {
        return path;
    }

    public SecureDataVersionRecord setPath(String path) {
        this.path = path;
        return this;
    }

    public String getEncryptedBlob() {
        return encryptedBlob;
    }

    public SecureDataVersionRecord setEncryptedBlob(String encryptedBlob) {
        this.encryptedBlob = encryptedBlob;
        return this;
    }

    public String getAction() {
        return action;
    }

    public SecureDataVersionRecord setAction(String action) {
        this.action = action;
        return this;
    }

    public String getVersionCreatedBy() {
        return versionCreatedBy;
    }

    public SecureDataVersionRecord setVersionCreatedBy(String versionCreatedBy) {
        this.versionCreatedBy = versionCreatedBy;
        return this;
    }

    public OffsetDateTime getVersionCreatedTs() {
        return versionCreatedTs;
    }

    public SecureDataVersionRecord setVersionCreatedTs(OffsetDateTime versionCreatedTs) {
        this.versionCreatedTs = versionCreatedTs;
        return this;
    }

    public String getActionPrincipal() {
        return actionPrincipal;
    }

    public SecureDataVersionRecord setActionPrincipal(String actionPrincipal) {
        this.actionPrincipal = actionPrincipal;
        return this;
    }

    public OffsetDateTime getActionTs() {
        return actionTs;
    }

    public SecureDataVersionRecord setActionTs(OffsetDateTime actionTs) {
        this.actionTs = actionTs;
        return this;
    }

    public enum SecretsAction {
        UPDATE,
        DELETE
    }
}