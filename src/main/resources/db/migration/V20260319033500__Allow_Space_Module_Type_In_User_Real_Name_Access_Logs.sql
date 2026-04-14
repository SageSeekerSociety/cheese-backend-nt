ALTER TABLE public.user_real_name_access_logs
    DROP CONSTRAINT IF EXISTS user_real_name_access_logs_module_type_check;

ALTER TABLE public.user_real_name_access_logs
    ADD CONSTRAINT user_real_name_access_logs_module_type_check CHECK (
        module_type IS NULL OR module_type IN ('TASK', 'SPACE')
    );
