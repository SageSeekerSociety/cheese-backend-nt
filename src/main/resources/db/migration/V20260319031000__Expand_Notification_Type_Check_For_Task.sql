ALTER TABLE public.notification
    DROP CONSTRAINT IF EXISTS notification_type_check;

ALTER TABLE public.notification
    ADD CONSTRAINT notification_type_check CHECK (
        type::text = ANY (
            ARRAY [
                'MENTION',
                'REPLY',
                'REACTION',
                'PROJECT_INVITE',
                'DEADLINE_REMIND',
                'TASK_PENDING_APPROVAL',
                'TASK_APPROVED',
                'TASK_REJECTED',
                'TASK_RESUBMITTED',
                'TASK_PARTICIPANT_APPLIED',
                'TASK_PARTICIPANT_APPROVED',
                'TASK_PARTICIPANT_REJECTED',
                'TASK_PARTICIPANT_AUTO_REJECTED',
                'TASK_SUBMISSION_CREATED',
                'TASK_SUBMISSION_UPDATED',
                'TASK_SUBMISSION_APPROVED',
                'TASK_SUBMISSION_REJECTED',
                'TEAM_JOIN_REQUEST',
                'TEAM_INVITATION',
                'TEAM_REQUEST_APPROVED',
                'TEAM_REQUEST_REJECTED',
                'TEAM_INVITATION_ACCEPTED',
                'TEAM_INVITATION_DECLINED',
                'TEAM_INVITATION_CANCELED',
                'TEAM_REQUEST_CANCELED'
            ]::text[]
        )
    );
