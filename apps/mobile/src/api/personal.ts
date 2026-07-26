import type { PersonalOverview, PersonalProfile } from "@/types/domain";
import { request } from "@/utils/http";

export const getPersonalProfile = () => request<PersonalOverview>({ url: "/personal" });
export const updatePersonalProfile = (data: { displayName: string; phone?: string; email?: string }) => request<PersonalProfile>({ url: "/personal/profile", method: "PUT", data });
export const updatePassword = (data: { currentPassword: string; newPassword: string }) => request<void>({ url: "/personal/password", method: "PUT", data });
