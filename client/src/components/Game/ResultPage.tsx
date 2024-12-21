import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import Editor from "@monaco-editor/react";
import { useGlobalState } from "./GlobalStateProvider";
import { queryAPI } from "../../api.ts";

interface ResultPageProps {
  winner: boolean;
  timeTaken: string;
  problemID: string;
  userID: string;
}

// no solution
// shows winner / loser screen
export default function ResultPage({
  winner,
  timeTaken,
  problemID,
  userID,
}: ResultPageProps) {
  const navigate = useNavigate();
  const editorRef = useRef();
  const [value, setValue] = useState("");
  const state = useGlobalState();

  // update state.code to newest value
  useEffect(() => {
    setValue(
      state.code?.trim() !== ""
        ? state.code
        : state.problem.signature[state.language]
    );
  }, [state.code]);

  // set up code editor
  function onMount(editor: { focus: () => void } | undefined) {
    editorRef.current = editor as any;
    if (editor) {
      editor.focus();
    }
  }

  // use UserUpdateHist to add code to database
  async function updateHist() {
    try {
      queryAPI("UserUpdateHist", {
        userID: userID,
        problemID: problemID,
        date: new Date().toISOString(),
        score: state.score,
        code: state.code,
        win: winner.toString(),
      });
    } catch {
      console.error("Error updating user history");
    }
  }

  // use RoomDel endpoint to delete a room after game finish
  async function deleteRoom() {
    try {
      const response = await queryAPI("RoomDel", { roomID: state.roomID });
      if (response.response_type === "success") {
        console.log("Room deleted successfully");
      } else {
        console.error("Failed to delete room");
      }
    } catch (error) {
      console.error("Error deleting room:", error);
    }
  }

  // remove all localStorage items, except for guestname if applicable and return to lobby
  function handleLeave() {
    const guestName = localStorage.getItem("guestName");
    localStorage.clear();
    if (guestName) {
      localStorage.setItem("guestName", guestName);
    }
    updateHist();
    deleteRoom();
    navigate("/lobby");
  }

  return (
    <div className="absolute fixed inset-0 h-screen w-screen bg-black z-30 bg-opacity-60 flex justify-center items-center">
      <div className="relative bg-custom-radial from-green-800 to-green-950 w-3/5 h-4/5 z-40 rounded-xl flex flex-col justify-center items-center gap-6">
        <h1 className="font-courier text-4xl font-bold text-gray-400 pt-2 text-glow">
          {winner ? "You Won!" : "You Lost!"}
        </h1>
        <p className="font-montserrat text-lg font-bold text-gray-400">
          You solved {state.score} test cases in {timeTaken}. Nice work!
        </p>
        <div className="bg-[#1E1E1E] h-3/5 w-4/5 rounded-lg">
          <div className="overflow-hidden h-full">
            <Editor
              options={{
                padding: {
                  top: 10,
                  bottom: 10,
                },
                minimap: {
                  enabled: false,
                },
                readOnly: true,
                scrollBeyondLastLine: false,
              }}
              theme="vs-dark"
              value={value}
              language={state.language}
              onMount={onMount}
              className="editor-wrapper"
            />
          </div>
        </div>
        <div className="flex flex-row justify-center gap-6 pb-2">
          <button
            className="flex font-courier text-gray-400 rounded-xl shadow-lg bg-red-500 p-[1px] hover:shadow-xl"
            onClick={handleLeave}
          >
            <div className="flex justify-center bg-red-700 py-3 px-4 rounded-xl shadow-lg hover:bg-red-800 text-xl font-bold">
              Leave Game
            </div>
          </button>
        </div>
      </div>
    </div>
  );
}

// if we had a solution
// export default function ResultPage({ winner, timeTaken }: ResultPageProps) {
//   const navigate = useNavigate();
//   const [view, setView] = useState<"code" | "solution">("code");
//   const editorRef = useRef();
//   const [value, setValue] = useState("");
//   const state = useGlobalState();

//   function onMount(editor: { focus: () => void } | undefined) {
//     editorRef.current = editor as any;
//     if (editor) {
//       editor.focus();
//     }
//   }

//   function toggleView() {
//     setView((prevView) => (prevView === "code" ? "solution" : "code"));
//   }

//   useEffect(() => {
//     setValue(view === "code" ? state.code : state.solution);
//   }, [view]);

//   async function deleteRoom() {
//     try {
//       const response = await queryAPI("RoomDel", { roomID: state.roomID });
//       if (response.response_type === "success") {
//         console.log("Room deleted successfully");
//       } else {
//         console.error("Failed to delete room");
//       }
//     } catch (error) {
//       console.error("Error deleting room:", error);
//     }
//   }
//   function handleLeave() {
//     localStorage.clear();
//     deleteRoom();
//     navigate("/lobby");
//   }

//   return (
//     <div className="absolute fixed inset-0 h-screen w-screen bg-black z-30 bg-opacity-60 flex justify-center items-center">
//       <div className="relative bg-custom-radial from-green-800 to-green-950 w-3/5 h-4/5 z-40 rounded-xl flex flex-col justify-center items-center gap-6">
//         <h1 className="font-courier text-4xl font-bold text-gray-400 pt-2 text-glow">
//           {winner ? "You Won!" : "You Lost!"}
//         </h1>
//         <p className="font-montserrat text-lg font-bold text-gray-400">
//           You solved {state.score} test cases in {timeTaken}. Nice work!
//         </p>
//         <div className="bg-[#1E1E1E] h-3/5 w-4/5 rounded-lg">
//           <div className="overflow-hidden h-full">
//             <Editor
//               options={{
//                 padding: {
//                   top: 10,
//                   bottom: 10,
//                 },
//                 minimap: {
//                   enabled: false,
//                 },
//                 readOnly: true,
//                 scrollBeyondLastLine: false,
//               }}
//               theme="vs-dark"
//               defaultValue={state.code}
//               language={state.language}
//               onMount={onMount}
//               value={value}
//               onChange={(value) => value !== undefined && setValue(value)}
//               className="editor-wrapper"
//             />
//           </div>
//         </div>
//         <div className="flex flex-row justify-center gap-6 pb-2">
//           <button
//             className="flex font-courier text-gray-400 rounded-xl shadow-lg bg-red-500 p-[1px] hover:shadow-xl"
//             onClick={handleLeave}
//           >
//             <div className="flex justify-center bg-red-700 py-3 px-4 rounded-xl shadow-lg hover:bg-red-800 text-xl font-bold">
//               Leave Game
//             </div>
//           </button>

//           <button
//             className="flex font-courier text-gray-400 rounded-xl shadow-lg bg-gray-300 p-[1px] hover:shadow-xl"
//             onClick={toggleView}
//           >
//             <div className="flex justify-center bg-stone-800 py-3 px-6 rounded-xl shadow-lg hover:bg-gray-700 text-xl font-bold">
//               {view === "code" ? "View Solution" : "View My Code"}
//             </div>
//           </button>
//         </div>
//       </div>
//     </div>
//   );
// }
