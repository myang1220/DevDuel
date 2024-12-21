import { useState, useEffect } from "react";
import { useUser } from "@clerk/clerk-react";
import { queryAPI } from "../../api";
import CodePanel from "./CodePanel";

interface CodeSubmission {
  problemName: string;
  code: string;
  date: string;
  score: string;
}

// shows all past problem panels
export default function PastCode() {
  const { user } = useUser();
  const [submissions, setSubmissions] = useState<CodeSubmission[]>([]);
  const [names, setNames] = useState<string[]>([]);
  const [fetchedProblems, setFetchedProblems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // get problem name and problem object for each submission
  useEffect(() => {
    const fetchProblemNames = async () => {
      try {
        const promises = submissions.map(async (submission) => {
          const response = await queryAPI("getproblem", {
            problemID: submission.problemName,
          });

          if (response.response_type === "success") {
            return {
              name: response.body[0].name,
              problem: response.body[0],
            };
          } else {
            setError("Failed to fetch problem names");
            return { name: "", problem: null };
          }
        });

        // Wait for all promises to resolve
        const results = await Promise.all(promises);

        // Extract names and problems in order
        const updatedNames = results.map((result) => result.name);
        const updatedProblems = results.map((result) => result.problem);

        setNames(updatedNames);
        setFetchedProblems(updatedProblems);
      } catch {
        setError("Error fetching problem names");
      }
    };

    fetchProblemNames();
  }, [submissions]);

  // use UserInfo endpoint to get user stats and code
  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        if (!user?.id) return;

        const response = await queryAPI("UserInfo", { userID: user.id });
        if (response.response_type === "success") {
          const codeData = response.data.code || {};

          const parsedSubmissions: CodeSubmission[] = Object.entries(
            codeData
          ).map(([problemKey, submission]) => ({
            problemName: problemKey.replace("problem", ""),
            code: (submission as any).code,
            date: (submission as any).date,
            score: (submission as any).score,
          }));

          setSubmissions(parsedSubmissions);
        } else {
          setError("Failed to fetch code submissions");
        }
      } catch (err) {
        console.error("Error fetching user info:", err);
        setError("An error occurred while fetching user data.");
      } finally {
        setLoading(false);
      }
    };

    fetchUserInfo();
  }, [user]);

  if (loading) {
    return <div className="text-center p-4">Loading submissions...</div>;
  }

  if (error) {
    return <div className="text-center text-red-500 p-4">{error}</div>;
  }

  return (
    <div className="w-full h-full flex flex-col">
      <div className="flex-grow overflow-y-auto p-8 overscroll-none waitingroom-scrollbar">
        <div className="flex flex-wrap justify-around gap-8 items-stretch">
          {names && fetchedProblems && submissions.length > 0 ? (
            submissions.map((submission, index) => (
              <CodePanel
                key={index}
                name={`${names[index]}`}
                numSolved={submission.score}
                code={submission.code}
                problemID={submission.problemName}
              />
            ))
          ) : (
            <div className="text-gray-400 text-center w-full">
              No past code submissions found.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
