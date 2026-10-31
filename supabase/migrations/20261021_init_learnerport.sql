-- ==============================================================================
-- LEARNERPORT DATABASE INITIALIZATION MIGRATION
-- Educational Content-Sharing Platform
-- ==============================================================================

-- 1. EXTENSIONS
create extension if not exists "uuid-ossp";

-- 2. PROFILES TABLE
create table if not exists public.profiles (
  id uuid references auth.users on delete cascade primary key,
  username text unique not null,
  display_name text not null,
  avatar_url text default '',
  bio text default '',
  date_of_birth date,
  educational_level text default '',
  course_or_degree text default '',
  field_of_study text default '',
  is_profile_completed boolean default false not null,
  preferred_language text default 'English',
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

-- Ensure columns exist if table was already created
alter table public.profiles add column if not exists date_of_birth date;
alter table public.profiles add column if not exists educational_level text default '';
alter table public.profiles add column if not exists course_or_degree text default '';
alter table public.profiles add column if not exists field_of_study text default '';
alter table public.profiles add column if not exists is_profile_completed boolean default false;

create index if not exists idx_profiles_username on public.profiles(username);

-- Trigger to auto-create profile on Supabase auth.users signup
create or replace function public.handle_new_user()
returns trigger as $$
declare
  raw_username text;
  raw_display text;
begin
  raw_username := coalesce(new.raw_user_meta_data->>'username', split_part(new.email, '@', 1));
  raw_display := coalesce(new.raw_user_meta_data->>'display_name', split_part(new.email, '@', 1));

  -- Ensure uniqueness of username
  if exists (select 1 from public.profiles where username = raw_username) then
    raw_username := raw_username || '_' || substr(new.id::text, 1, 4);
  end if;

  insert into public.profiles (id, username, display_name, avatar_url, is_profile_completed)
  values (
    new.id,
    raw_username,
    raw_display,
    coalesce(new.raw_user_meta_data->>'avatar_url', ''),
    false
  )
  on conflict (id) do nothing;
  return new;
end;
$$ language plpgsql security definer;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute procedure public.handle_new_user();

-- 3. RESOURCES TABLE
create table if not exists public.resources (
  id uuid default gen_random_uuid() primary key,
  uploader_id uuid references public.profiles(id) on delete cascade not null,
  content_type text not null check (content_type in ('NOTES', 'BOOK', 'QUESTION_PAPER', 'CODING_RESOURCE')),
  title text not null,
  description text not null,
  subject text not null,
  topic text not null,
  educational_level text not null,
  class_or_course text default '',
  board_or_university text default '',
  language text default 'English',
  tags text[] default '{}',
  file_path text default '',
  thumbnail_path text default '',
  file_size text default '2.4 MB',
  file_type text default 'PDF',
  publication_status text default 'published' check (publication_status in ('draft', 'published', 'archived')),
  paper_type text,
  examination_year text,
  pages_content jsonb default '[]'::jsonb,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create index if not exists idx_resources_uploader on public.resources(uploader_id);
create index if not exists idx_resources_type on public.resources(content_type);
create index if not exists idx_resources_subject on public.resources(subject);
create index if not exists idx_resources_created_at on public.resources(created_at desc);

-- 4. THOUGHTS TABLE
create table if not exists public.thoughts (
  id uuid default gen_random_uuid() primary key,
  author_id uuid references public.profiles(id) on delete cascade not null,
  title text,
  body text not null,
  image_path text,
  tags text[] default '{}',
  publication_status text default 'published' check (publication_status in ('draft', 'published', 'archived')),
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create index if not exists idx_thoughts_author on public.thoughts(author_id);
create index if not exists idx_thoughts_created_at on public.thoughts(created_at desc);

-- 5. FOLLOWS TABLE
create table if not exists public.follows (
  id uuid default gen_random_uuid() primary key,
  follower_id uuid references public.profiles(id) on delete cascade not null,
  following_id uuid references public.profiles(id) on delete cascade not null,
  created_at timestamptz default now() not null,
  constraint uq_follow unique (follower_id, following_id),
  constraint chk_no_self_follow check (follower_id <> following_id)
);

create index if not exists idx_follows_follower on public.follows(follower_id);
create index if not exists idx_follows_following on public.follows(following_id);

-- 6. SAVED RESOURCES TABLE
create table if not exists public.saved_resources (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references public.profiles(id) on delete cascade not null,
  resource_id uuid references public.resources(id) on delete cascade,
  thought_id uuid references public.thoughts(id) on delete cascade,
  created_at timestamptz default now() not null,
  constraint chk_saved_target check (
    (resource_id is not null and thought_id is null) or
    (resource_id is null and thought_id is not null)
  )
);

create unique index if not exists uq_saved_resource on public.saved_resources (user_id, resource_id) where resource_id is not null;
create unique index if not exists uq_saved_thought on public.saved_resources (user_id, thought_id) where thought_id is not null;

-- 7. RESOURCE VIEWS
create table if not exists public.resource_views (
  id uuid default gen_random_uuid() primary key,
  resource_id uuid references public.resources(id) on delete cascade not null,
  viewer_id uuid references public.profiles(id) on delete set null,
  created_at timestamptz default now() not null
);

create index if not exists idx_views_resource on public.resource_views(resource_id);

-- 8. RESOURCE DOWNLOADS
create table if not exists public.resource_downloads (
  id uuid default gen_random_uuid() primary key,
  resource_id uuid references public.resources(id) on delete cascade not null,
  user_id uuid references public.profiles(id) on delete cascade not null,
  created_at timestamptz default now() not null
);

create index if not exists idx_downloads_resource on public.resource_downloads(resource_id);
create index if not exists idx_downloads_user on public.resource_downloads(user_id);

-- 9. RESOURCE RATINGS
create table if not exists public.resource_ratings (
  id uuid default gen_random_uuid() primary key,
  resource_id uuid references public.resources(id) on delete cascade not null,
  user_id uuid references public.profiles(id) on delete cascade not null,
  rating int not null check (rating >= 1 and rating <= 5),
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null,
  constraint uq_user_resource_rating unique (resource_id, user_id)
);

create index if not exists idx_ratings_resource on public.resource_ratings(resource_id);

-- 10. RESOURCE COMMENTS
create table if not exists public.resource_comments (
  id uuid default gen_random_uuid() primary key,
  resource_id uuid references public.resources(id) on delete cascade not null,
  author_id uuid references public.profiles(id) on delete cascade not null,
  body text not null check (length(trim(body)) > 0),
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create index if not exists idx_comments_resource on public.resource_comments(resource_id);

-- 11. THOUGHT LIKES
create table if not exists public.thought_likes (
  id uuid default gen_random_uuid() primary key,
  thought_id uuid references public.thoughts(id) on delete cascade not null,
  user_id uuid references public.profiles(id) on delete cascade not null,
  created_at timestamptz default now() not null,
  constraint uq_thought_like unique (thought_id, user_id)
);

create index if not exists idx_likes_thought on public.thought_likes(thought_id);

-- 12. THOUGHT COMMENTS
create table if not exists public.thought_comments (
  id uuid default gen_random_uuid() primary key,
  thought_id uuid references public.thoughts(id) on delete cascade not null,
  author_id uuid references public.profiles(id) on delete cascade not null,
  body text not null check (length(trim(body)) > 0),
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

create index if not exists idx_comments_thought on public.thought_comments(thought_id);

-- 13. NOTIFICATIONS
create table if not exists public.notifications (
  id uuid default gen_random_uuid() primary key,
  user_id uuid references public.profiles(id) on delete cascade not null,
  type text not null,
  title text not null,
  message text not null,
  target_resource_id uuid,
  is_read boolean default false not null,
  created_at timestamptz default now() not null
);

create index if not exists idx_notifications_user on public.notifications(user_id, is_read);

-- 14. CONTENT REPORTS
create table if not exists public.content_reports (
  id uuid default gen_random_uuid() primary key,
  reporter_id uuid references public.profiles(id) on delete cascade not null,
  resource_id uuid references public.resources(id) on delete cascade,
  thought_id uuid references public.thoughts(id) on delete cascade,
  reason text not null,
  details text,
  status text default 'pending' check (status in ('pending', 'reviewed', 'dismissed', 'actioned')),
  created_at timestamptz default now() not null
);

-- ==============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

alter table public.profiles enable row level security;
alter table public.resources enable row level security;
alter table public.thoughts enable row level security;
alter table public.follows enable row level security;
alter table public.saved_resources enable row level security;
alter table public.resource_views enable row level security;
alter table public.resource_downloads enable row level security;
alter table public.resource_ratings enable row level security;
alter table public.resource_comments enable row level security;
alter table public.thought_likes enable row level security;
alter table public.thought_comments enable row level security;
alter table public.notifications enable row level security;
alter table public.content_reports enable row level security;

-- PROFILES RLS
create policy "Public profiles are viewable by everyone" on public.profiles
  for select using (true);

create policy "Users can update their own profile" on public.profiles
  for update using (auth.uid() = id);

create policy "Users can insert their own profile" on public.profiles
  for insert with check (auth.uid() = id);

-- RESOURCES RLS
create policy "Published resources are viewable by everyone" on public.resources
  for select using (publication_status = 'published' or auth.uid() = uploader_id);

create policy "Users can insert their own resources" on public.resources
  for insert with check (auth.uid() = uploader_id);

create policy "Users can update their own resources" on public.resources
  for update using (auth.uid() = uploader_id);

create policy "Users can delete their own resources" on public.resources
  for delete using (auth.uid() = uploader_id);

-- THOUGHTS RLS
create policy "Published thoughts are viewable by everyone" on public.thoughts
  for select using (publication_status = 'published' or auth.uid() = author_id);

create policy "Users can create their own thoughts" on public.thoughts
  for insert with check (auth.uid() = author_id);

create policy "Users can update their own thoughts" on public.thoughts
  for update using (auth.uid() = author_id);

create policy "Users can delete their own thoughts" on public.thoughts
  for delete using (auth.uid() = author_id);

-- FOLLOWS RLS
create policy "Follows viewable by everyone" on public.follows
  for select using (true);

create policy "Users can follow others" on public.follows
  for insert with check (auth.uid() = follower_id and follower_id <> following_id);

create policy "Users can unfollow" on public.follows
  for delete using (auth.uid() = follower_id);

-- SAVED RESOURCES RLS
create policy "Users can view only their own saved items" on public.saved_resources
  for select using (auth.uid() = user_id);

create policy "Users can save resources" on public.saved_resources
  for insert with check (auth.uid() = user_id);

create policy "Users can unsave resources" on public.saved_resources
  for delete using (auth.uid() = user_id);

-- RESOURCE VIEWS RLS
create policy "Views viewable by everyone or creator" on public.resource_views
  for select using (true);

create policy "Anyone can record a resource view" on public.resource_views
  for insert with check (true);

-- RESOURCE DOWNLOADS RLS
create policy "Users can view their own downloads or resource owners" on public.resource_downloads
  for select using (
    auth.uid() = user_id or
    auth.uid() in (select uploader_id from public.resources where id = resource_id)
  );

create policy "Users can record downloads" on public.resource_downloads
  for insert with check (auth.uid() = user_id);

-- RATINGS RLS
create policy "Ratings viewable by everyone" on public.resource_ratings
  for select using (true);

create policy "Users can rate resources" on public.resource_ratings
  for insert with check (auth.uid() = user_id);

create policy "Users can update their own rating" on public.resource_ratings
  for update using (auth.uid() = user_id);

-- COMMENTS RLS
create policy "Resource comments are viewable by everyone" on public.resource_comments
  for select using (true);

create policy "Users can post resource comments" on public.resource_comments
  for insert with check (auth.uid() = author_id);

create policy "Users can delete their own comments" on public.resource_comments
  for delete using (auth.uid() = author_id);

-- THOUGHT LIKES RLS
create policy "Likes viewable by everyone" on public.thought_likes
  for select using (true);

create policy "Users can like thoughts" on public.thought_likes
  for insert with check (auth.uid() = user_id);

create policy "Users can unlike thoughts" on public.thought_likes
  for delete using (auth.uid() = user_id);

-- THOUGHT COMMENTS RLS
create policy "Thought comments viewable by everyone" on public.thought_comments
  for select using (true);

create policy "Users can post thought comments" on public.thought_comments
  for insert with check (auth.uid() = author_id);

create policy "Users can delete their thought comments" on public.thought_comments
  for delete using (auth.uid() = author_id);

-- NOTIFICATIONS RLS
create policy "Users can view only their own notifications" on public.notifications
  for select using (auth.uid() = user_id);

create policy "Users can update read status on their notifications" on public.notifications
  for update using (auth.uid() = user_id);

-- CONTENT REPORTS RLS
create policy "Users can submit reports" on public.content_reports
  for insert with check (auth.uid() = reporter_id);

-- ==============================================================================
-- STORAGE BUCKETS SETUP & STORAGE POLICIES
-- ==============================================================================

insert into storage.buckets (id, name, public)
values
  ('avatars', 'avatars', true),
  ('resource-files', 'resource-files', true),
  ('resource-thumbnails', 'resource-thumbnails', true),
  ('thought-images', 'thought-images', true)
on conflict (id) do nothing;

-- Storage Policies for Avatars
create policy "Avatar images are publicly accessible" on storage.objects
  for select using (bucket_id = 'avatars');

create policy "Users can upload their own avatar" on storage.objects
  for insert with check (bucket_id = 'avatars' and auth.uid()::text = (storage.foldername(name))[1]);

create policy "Users can update their own avatar" on storage.objects
  for update using (bucket_id = 'avatars' and auth.uid()::text = (storage.foldername(name))[1]);

-- Storage Policies for Educational Resources
create policy "Resource files are publicly accessible" on storage.objects
  for select using (bucket_id in ('resource-files', 'resource-thumbnails', 'thought-images'));

create policy "Authenticated users can upload educational files" on storage.objects
  for insert with check (bucket_id in ('resource-files', 'resource-thumbnails', 'thought-images') and auth.role() = 'authenticated');
