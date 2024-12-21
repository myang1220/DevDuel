import { queryAPI } from "../../api.ts";

const languages = {
  python: "",
  javascript: "",
  java: "",
};

export type Language = keyof typeof languages;

let cachedLanguages: Promise<Record<string, string>> | null = null;

// use runtimes endpoint to get available language versions
export async function fetchLanguages(): Promise<Record<string, string>> {
  if (!cachedLanguages) {
    cachedLanguages = queryAPI("runtimes").then((data) => {
      if (data.response_type !== "success" || !Array.isArray(data.body)) {
        throw new Error("Unexpected API response format");
      }

      // Transform the array into a key-value map
      const versions: Record<string, string> = {};
      for (const runtime of data.body) {
        if (runtime.language && runtime.version) {
          versions[runtime.language] = runtime.version;
        }
      }
      return versions;
    });
  }
  return cachedLanguages;
}

export const LANGUAGE_VERSIONS = await fetchLanguages();
