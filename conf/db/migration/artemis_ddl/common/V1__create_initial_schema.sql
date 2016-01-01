/**
 * Copyright 2015 Groupon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
CREATE TABLE authentication (
    id ${idtype} primary key,
    user_name character varying(255),
    token character varying(255),
    unique (user_name)
);

CREATE TABLE bundle (
    id ${idtype} primary key,
    name character varying(255),
    owner_id bigint,
    unique (name)
);

CREATE TABLE deployment (
    id ${idtype} primary key,
    heartbeat timestamp,
    deployment_owner character varying(255),
    manifest_history_id bigint,
    state character varying(11),
    start timestamp,
    finished timestamp,
    initiator character varying(255),
    check (state in ('FAILED', 'RUNNING', 'NOT_STARTED', 'SUCCEEDED'))
);

CREATE TABLE deployment_log (
    id ${idtype} primary key,
    deployment_id bigint,
    host_id bigint,
    message text,
    log_time timestamp
);

CREATE TABLE environment (
    id ${idtype} primary key,
    name character varying(255),
    owner_id bigint,
    parent_id bigint,
    environment_type character varying(6),
    config text DEFAULT ''::text,
    version bigint DEFAULT 0,
    check (environment_type in ('ROLLER', 'DOCKER')),
    unique (name)
);

CREATE TABLE host (
    id ${idtype} primary key,
    name character varying(255),
    hostclass_id bigint,
    unique (name)
);

CREATE TABLE host_deployment (
    id ${idtype} primary key,
    host_id bigint,
    deployment_id bigint,
    state character varying(11),
    state_detail character varying(255),
    heartbeat timestamp,
    started timestamp,
    finished timestamp,
    check (state in ('FAILED', 'RUNNING', 'NOT_STARTED', 'SUCCEEDED'))
);

CREATE TABLE hostclass (
    id ${idtype} primary key,
    name character varying(255),
    parent_id bigint,
    unique (name)
);

CREATE TABLE hostclass_stage (
    hostclass_id bigint NOT NULL,
    stage_id bigint NOT NULL
);

CREATE TABLE inflight_deployment (
    id ${idtype} primary key
);

CREATE TABLE manifest (
    id ${idtype} primary key,
    created_by character varying(255),
    environment_id bigint,
    version character varying(64),
    created_at timestamp,
    unique (environment_id, version)
);

CREATE TABLE manifest_history (
    id ${idtype} primary key,
    manifest_id bigint,
    stage_id bigint,
    start timestamp,
    finish timestamp
);

CREATE TABLE manifest_package_version (
    manifest_id bigint NOT NULL,
    package_version_id bigint NOT NULL,
    primary key (manifest_id, package_version_id)
);

CREATE TABLE owner (
    id ${idtype} primary key,
    org_name character varying(255),
    unique (org_name)
);

CREATE TABLE package (
    id ${idtype} primary key,
    name character varying(255),
    unique (name)
);

CREATE TABLE package_version (
    id ${idtype} primary key,
    pkg_id bigint,
    type character varying(255),
    repository character varying(255),
    version character varying(255),
    description character varying(255),
    unique (repository, pkg_id, version)
);

CREATE TABLE stage (
    id ${idtype} primary key,
    name character varying(255),
    environment_id bigint,
    config text DEFAULT '',
    version bigint DEFAULT 0,
    unique (environment_id, name)
);

CREATE TABLE stage_hostclass (
    stage_id ${idreftype} NOT NULL,
    hostclass_id bigint NOT NULL
);

CREATE TABLE user_membership (
    id ${idtype} primary key,
    user_name character varying(255),
    org_id bigint,
    unique (user_name, org_id)
);

CREATE INDEX ON bundle (owner_id);
CREATE INDEX ON deployment_log (deployment_id);
CREATE INDEX ON deployment_log (host_id);
CREATE INDEX ON deployment (manifest_history_id);
CREATE INDEX ON environment (owner_id);
CREATE INDEX ON environment (parent_id);
CREATE INDEX ON host_deployment (deployment_id);
CREATE INDEX ON host_deployment (host_id);
CREATE INDEX ON host (hostclass_id);
CREATE INDEX ON hostclass (parent_id);
CREATE INDEX ON manifest (environment_id);
CREATE INDEX ON manifest (environment_id, created_at);
CREATE INDEX ON manifest_history (manifest_id);
CREATE INDEX ON manifest_history (stage_id);
CREATE INDEX ON package_version (pkg_id);
CREATE INDEX ON stage (environment_id);
CREATE INDEX ON user_membership (org_id);

ALTER TABLE bundle ADD FOREIGN KEY (owner_id) REFERENCES owner(id);
ALTER TABLE deployment_log ADD FOREIGN KEY (deployment_id) REFERENCES deployment(id);
ALTER TABLE deployment_log ADD FOREIGN KEY (host_id) REFERENCES host(id);
ALTER TABLE deployment ADD FOREIGN KEY (manifest_history_id) REFERENCES manifest_history(id);
ALTER TABLE environment ADD FOREIGN KEY (owner_id) REFERENCES owner(id);
ALTER TABLE environment ADD FOREIGN KEY (parent_id) REFERENCES environment(id);
ALTER TABLE host_deployment ADD FOREIGN KEY (deployment_id) REFERENCES deployment(id);
ALTER TABLE host_deployment ADD FOREIGN KEY (host_id) REFERENCES host(id);
ALTER TABLE host ADD FOREIGN KEY (hostclass_id) REFERENCES hostclass(id);
ALTER TABLE hostclass ADD FOREIGN KEY (parent_id) REFERENCES hostclass(id);
ALTER TABLE hostclass_stage ADD FOREIGN KEY (hostclass_id) REFERENCES hostclass(id);
ALTER TABLE hostclass_stage ADD FOREIGN KEY (stage_id) REFERENCES stage(id);
ALTER TABLE manifest ADD FOREIGN KEY (environment_id) REFERENCES environment(id);
ALTER TABLE manifest_history ADD FOREIGN KEY (manifest_id) REFERENCES manifest(id);
ALTER TABLE manifest_history ADD FOREIGN KEY (stage_id) REFERENCES stage(id);
ALTER TABLE manifest_package_version ADD FOREIGN KEY (manifest_id) REFERENCES manifest(id);
ALTER TABLE manifest_package_version ADD FOREIGN KEY (package_version_id) REFERENCES package_version(id);
ALTER TABLE package_version ADD FOREIGN KEY (pkg_id) REFERENCES package(id);
ALTER TABLE stage ADD FOREIGN KEY (environment_id) REFERENCES environment(id);
ALTER TABLE stage_hostclass ADD FOREIGN KEY (hostclass_id) REFERENCES hostclass(id);
ALTER TABLE stage_hostclass ADD FOREIGN KEY (stage_id) REFERENCES stage(id);
ALTER TABLE user_membership ADD FOREIGN KEY (org_id) REFERENCES owner(id);

