-- Captured from main b30e1c9 Hibernate 5.4 DDL; do not regenerate with upgraded ORM.
create sequence hibernate_sequence start with 1001 increment by 1;
create sequence member_sequence start with 1001 increment by 1;
create sequence que_sequence start with 1001 increment by 1;
create table announcement (
       id bigint not null,
        announcement_type varchar(255),
        max_career integer not null,
        min_career integer not null,
        name varchar(255),
        access_url varchar(255),
        content clob,
        payment integer not null,
        save_file_name varchar(255),
        upload_file_name varchar(255),
        job_position varchar(255),
        dead_line_date timestamp,
        started_date timestamp,
        title varchar(255),
        primary key (id)
    );
create table answer (
       answer_id bigint not null,
        content clob,
        dislike integer not null,
        recommend integer not null,
        member_id bigint,
        question_id bigint,
        primary key (answer_id)
    );
create table hashtag (
       hashtag_id bigint not null,
        tag_name varchar(255) not null,
        primary key (hashtag_id)
    );
create table language (
       announcement_id bigint not null,
        languages varchar(255)
    );
create table member (
       member_id bigint not null,
        password varchar(255),
        username varchar(255) not null,
        primary key (member_id)
    );
create table member_roles (
       member_member_id bigint not null,
        roles varchar(255)
    );
create table question (
       question_id bigint not null,
        content clob,
        created_date timestamp,
        dislike integer not null,
        hits integer not null,
        recommend integer not null,
        title varchar(255),
        member_id bigint,
        primary key (question_id)
    );
create table question_hashtag (
       id bigint not null,
        hashtag_id bigint,
        question_id bigint,
        primary key (id)
    );
INSERT INTO hashtag (hashtag_id, tag_name) VALUES (1000, 'legacy-preserved');
