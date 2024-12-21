import { Editor } from "@monaco-editor/react";
import { useEffect, useRef, useState } from "react";
import { queryAPI } from "../../api";
import { Problem } from "../Game/GlobalStateProvider.tsx";

interface CodeReviewProps {
  onClose: () => void;
  name: string;
  code: string;
  score: string;
  problemID: string;
}

// review page for individual problems with code editor
export default function CodeReview({
  onClose,
  name,
  code,
  score,
  problemID,
}: CodeReviewProps) {
  const editorRef = useRef();
  const [leftWidth, setLeftWidth] = useState<number>(40);
  const [rightWidth, setRightWidth] = useState<number>(60);
  const [problem, setProblem] = useState<Problem>();
  const [language, setLanguage] = useState<string>("python"); // Default to python

  // set up splitter
  const handleMouseDown = (
    e: React.MouseEvent<HTMLDivElement>,
    direction: "vertical"
  ) => {
    e.preventDefault();

    const startX = e.clientX;
    const initialLeftWidth = leftWidth;

    const onMouseMove = (e: MouseEvent) => {
      if (direction === "vertical") {
        const deltaX = e.clientX - startX;
        const newLeftWidth = Math.max(
          30,
          Math.min(60, initialLeftWidth + (deltaX / window.innerWidth) * 100)
        );
        setLeftWidth(newLeftWidth);
        setRightWidth(100 - newLeftWidth);
      }
    };

    const onMouseUp = () => {
      document.removeEventListener("mousemove", onMouseMove);
      document.removeEventListener("mouseup", onMouseUp);
    };

    document.addEventListener("mousemove", onMouseMove);
    document.addEventListener("mouseup", onMouseUp);
  };

  // set up code editor
  function onMount(editor: { focus: () => void } | undefined) {
    editorRef.current = editor as any;
    if (editor) {
      editor.focus();
    }
  }

  // use getproblem endpoint to get problem information
  async function fetchProblem() {
    console.log(problemID);
    try {
      const response = await queryAPI("getproblem", {
        problemID: problemID,
      });
      if (response.response_type === "success") {
        const fetchedProblem = response.body[0];
        setProblem(fetchedProblem);
        detectLanguage(code);
      } else {
        console.error("Failed to fetch problem");
      }
    } catch (error) {
      console.error("Error fetching problem:", error);
    }
  }

  // set up code editor langauge based on code
  function detectLanguage(code: string) {
    if (code.includes("def")) {
      setLanguage("python");
      return;
    } else if (code.includes("class")) {
      setLanguage("java");
      return;
    } else if (code.includes("function")) {
      setLanguage("javascript");
      return;
    }
  }

  useEffect(() => {
    fetchProblem();
  }, []);

  if (!problem) {
    return <div>Failed to load problem.</div>;
  }

  const { description, tests } = problem;

  return (
    <div className="fixed top-0 left-0 w-full h-full bg-black bg-opacity-50 z-30 flex justify-center items-center">
      <div className="relative bg-custom-radial from-green-800 to-green-950 w-4/5 h-[90%] z-40 rounded-xl flex flex-col justify-between p-4">
        {/* Header */}
        <h2 className="flex justify-center items-center font-courier font-bold text-gray-400 text-4xl text-glow mt-2 mb-4">
          {name}
        </h2>

        <div className="flex justify-center items-center pb-4 text-gray-300">
          {eval(score) === 1
            ? `You had solved ${score} test cases. Nice work!`
            : `You had solved ${score} test cases. Review and try again!`}
        </div>

        {/* Main Content */}
        <div className="flex flex-grow overflow-hidden">
          {/* Left Panel */}
          <div
            className="bg-[#1E1E1E] rounded-lg p-2 overflow-y-scroll scrollable-content"
            style={{ width: `${leftWidth}%` }}
          >
            <div className="w-full h-full">
              <div className="p-2 font-montserrat font-normal">
                <h1 className="flex justify-center text-3xl font-bold py-2 font-courier">
                  {name}
                </h1>
                <div className="flex flex-col gap-4 pb-8 text-gray-200 text-sm">
                  {description.split("\\n").map((line, index) => (
                    <p key={index}>
                      {line.split("\\t").map((segment, i) => (
                        <span
                          key={i}
                          style={{ paddingLeft: i > 0 ? "1em" : "0" }}
                        >
                          {segment}
                        </span>
                      ))}
                    </p>
                  ))}
                </div>
                <div>
                  {Object.entries(tests).map(([key, test], index) => (
                    <div key={key} className="flex flex-col gap-4 text-sm">
                      <h3 className="font-semibold">Example {index + 1}</h3>
                      <div className="flex flex-row pb-4">
                        <div className="w-[2px] bg-gray-600"></div>
                        <div className="font-courier text-xs">
                          <p className="pl-6 text-gray-400">
                            <strong className="text-gray-200">Input:</strong>{" "}
                            {test.params}
                          </p>
                          <p className="pl-6 text-gray-400">
                            <strong className="text-gray-200">Output:</strong>{" "}
                            {test.expected}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* Vertical Divider */}
          <div
            className="cursor-col-resize bg-gray-500 w-[5px] mx-1 rounded-lg"
            onMouseDown={(e) => handleMouseDown(e, "vertical")}
          />

          {/* Right Panel */}
          <div
            className="flex justify-center items-center"
            style={{ width: `${rightWidth}%` }}
          >
            <Editor
              value={code}
              language={language}
              theme="vs-dark"
              onMount={onMount}
              options={{
                minimap: { enabled: false },
                scrollBeyondLastLine: false,
                padding: { top: 10, bottom: 10 },
              }}
              className="editor-wrapper w-full h-full"
            />
          </div>
        </div>

        {/* Footer Button */}
        <div className="w-full flex flex-shrink-0 justify-center p-4 mt-2">
          <button
            className="font-courier text-gray-400 rounded-xl shadow-lg bg-gray-300 p-[1px] hover:shadow-xl"
            onClick={onClose}
          >
            <div className="flex justify-center bg-stone-800 py-3 px-4 rounded-xl shadow-lg hover:bg-gray-700 text-xl font-bold">
              Exit Review
            </div>
          </button>
        </div>
      </div>
    </div>
  );
}
