import { request } from "@/utils/request";

export interface SchoolItem {
  id: number;
  name: string;
}

export interface CampusItem {
  id: number;
  schoolId: number;
  name: string;
}

export interface CourseItem {
  id: number;
  schoolId: number;
  courseCode: string;
  courseName: string;
}

export function listSchools() {
  return request<SchoolItem[]>({ url: "/api/schools", method: "GET" });
}

export function listCampuses(schoolId?: number) {
  return request<CampusItem[]>({
    url: "/api/campuses",
    method: "GET",
    data: schoolId ? { schoolId } : undefined
  });
}

export function listCourses(schoolId?: number) {
  return request<CourseItem[]>({
    url: "/api/courses",
    method: "GET",
    data: schoolId ? { schoolId } : undefined
  });
}
