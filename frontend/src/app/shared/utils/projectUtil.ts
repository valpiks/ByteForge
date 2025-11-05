export interface PermissionGroup {
  title: string
  permissions: string[]
  icon?: string
}

export const getRolePermissions = (role: string): PermissionGroup[] => {
  switch (role?.toLowerCase()) {
    case 'owner':
      return [
        {
          title: 'Project Management',
          permissions: ['Delete project', 'Transfer ownership', 'Manage project settings'],
          icon: '⚙️',
        },
        {
          title: 'Team Management',
          permissions: [
            'Invite new members',
            'Remove team members',
            'Change user roles',
            'Manage permissions',
          ],
          icon: '👥',
        },
        {
          title: 'Full Access',
          permissions: [
            'Create and edit files',
            'Execute and debug code',
            'Rename and delete files',
            'Access all project features',
          ],
          icon: '🔑',
        },
      ]

    case 'developer':
      return [
        {
          title: 'Development Access',
          permissions: [
            'Create and edit files',
            'Execute and debug code',
            'Rename and delete files',
            'Upload and download files',
          ],
          icon: '💻',
        },
        {
          title: 'Limitations',
          permissions: [
            'Cannot manage team members',
            'Cannot change project settings',
            'Cannot delete project',
          ],
          icon: '🚫',
        },
      ]

    case 'viewer':
      return [
        {
          title: 'View Access',
          permissions: ['View all files', 'Download files', 'Read project content'],
          icon: '👀',
        },
        {
          title: 'Restrictions',
          permissions: [
            'Cannot edit files',
            'Cannot execute code',
            'Cannot modify project',
            'Cannot manage team',
          ],
          icon: '🔒',
        },
      ]

    default:
      return [
        {
          title: 'Basic Access',
          permissions: ['View project content'],
          icon: '📄',
        },
      ]
  }
}
