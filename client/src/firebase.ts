import { initializeApp } from "firebase/app";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
  apiKey: "AIzaSyAzA8UO7Yv-ImL4Whj9sO6nelDEphNSdE4",
  authDomain: "devduel-68bdc.firebaseapp.com",
  projectId: "devduel-68bdc",
  storageBucket: "devduel-68bdc.firebasestorage.app",
  messagingSenderId: "301469781851",
  appId: "1:301469781851:web:8940c812b21236ce16947e",
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

export { db, app };
