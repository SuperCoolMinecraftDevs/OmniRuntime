wit_bindgen::generate!({
    path: "wit",
    world: "greeter",
});

struct Greeter;

impl Guest for Greeter {
    fn greet(name: String) -> String {
        format!("Hello, {name}!")
    }

    fn checksum(data: Vec<u8>) -> u32 {
        data.iter().fold(0u32, |acc, b| acc.wrapping_mul(31).wrapping_add(*b as u32))
    }
}

export!(Greeter);
