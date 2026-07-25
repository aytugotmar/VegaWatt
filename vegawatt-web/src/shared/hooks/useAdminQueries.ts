import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { fetchAdminUsers, updateUserRole } from "../api/adminApi";

const ADMIN_USERS_QUERY_KEY = ["admin", "users"];

export function useAdminUsersQuery(enabled: boolean) {
  return useQuery({
    queryKey: ADMIN_USERS_QUERY_KEY,
    queryFn: fetchAdminUsers,
    enabled,
  });
}

export function useUpdateUserRoleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: "ADMIN" | "USER" }) => updateUserRole(userId, role),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_USERS_QUERY_KEY });
    },
  });
}
