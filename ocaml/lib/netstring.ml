(*
 * Copyright (c) 2003  Dustin Sallings <dustin@spy.net>
 *
 * arch-tag: D427C96F-2C1F-11D8-82B6-000393CFE6B8
 *)

(** A netstring implementation. *)

(** Encode a string into a netstring. *)
let encode s =
	(string_of_int (String.length s)) ^ ":" ^ s ^ ","
;;

(* The maximum length of a string we'll read (999,999,999 bytes) *)
let max_string_size = 999999999;;

(** Get the next netstring from the given stream. *)
let decode in_stream =
	let rec loop l =
		if (List.length l > 9) then failwith "Size too long";
		let c = Stream.next in_stream in
		if (c = ':') then (
			int_of_string (Extstring.string_of_chars l)
		) else (
			loop (l @ [c])
		) in
	let size = loop [] in
	if (size > max_string_size) then
		failwith ("Won't read a nestring of length " ^ (string_of_int size));
	let data = Stream.npeek size in_stream in
	List.iter (fun _ -> Stream.junk in_stream) data;
	let comma = Stream.next in_stream in
	if (comma != ',') then (
		failwith "Invalid netstring (didn't get a comma in the right place)"
	);
	Extstring.string_of_chars data
;;