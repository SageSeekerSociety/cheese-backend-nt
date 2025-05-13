ALTER TABLE task_membership_team_members
    ALTER COLUMN participant_member_uuid SET NOT NULL;

ALTER TABLE task_membership
    ALTER COLUMN participant_uuid SET NOT NULL;