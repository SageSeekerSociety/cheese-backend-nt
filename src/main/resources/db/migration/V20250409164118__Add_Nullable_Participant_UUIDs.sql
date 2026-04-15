ALTER TABLE task_membership_team_members
    ADD participant_member_uuid UUID;

ALTER TABLE task_membership
    ADD participant_uuid UUID;

ALTER TABLE task_membership
    ADD reject_reason VARCHAR(255);